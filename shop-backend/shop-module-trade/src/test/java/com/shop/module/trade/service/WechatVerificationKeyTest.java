package com.shop.module.trade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.config.WechatPayProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class WechatVerificationKeyTest {
    @TempDir Path directory;

    @Test void verifiesRealRsaSignatureAndRejectsTamperedBodyOrUnknownKey() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keys = generator.generateKeyPair();
        Path file = directory.resolve("wechat-public.pem");
        Files.writeString(file, "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(keys.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----");
        var config = new WechatPayProperties();
        config.setPublicKeyId("PUB_KEY_ID_TEST");
        config.setPublicKeyPath(file.toString());
        var service = new WechatPayService(config, new ObjectMapper(), mock(JdbcTemplate.class));
        var signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keys.getPrivate());
        signer.update("signed-body".getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "verifySignature", "signed-body", signature, "PUB_KEY_ID_TEST"));
        assertThrows(ServerException.class, () -> ReflectionTestUtils.invokeMethod(service, "verifySignature", "changed-body", signature, "PUB_KEY_ID_TEST"));
        assertThrows(ServerException.class, () -> ReflectionTestUtils.invokeMethod(service, "verifySignature", "signed-body", signature, "PUB_KEY_ID_OTHER"));
    }
}
