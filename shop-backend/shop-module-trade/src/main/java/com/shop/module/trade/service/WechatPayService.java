package com.shop.module.trade.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.config.WechatPayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WechatPayService {

    private static final String API_BASE_URL = "https://api.mch.weixin.qq.com";
    private static final String JSAPI_PATH = "/v3/pay/transactions/jsapi";

    private final WechatPayProperties properties;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public Map<String, Object> createMiniProgramPayment(TradeOrderDOView order, String paySn) {
        validateConfiguration();
        String openid = jdbcTemplate.queryForObject(
                "SELECT openid FROM member_user WHERE id = ? AND status = 1 AND deleted = b'0'",
                String.class, order.userId());
        if (openid == null || openid.isBlank()) {
            throw new ServerException(400, "微信用户身份无效，请重新登录");
        }
        try {
            Map<String, Object> amount = Map.of("total", order.amount(), "currency", "CNY");
            Map<String, Object> payer = Map.of("openid", openid);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("appid", properties.getAppId());
            payload.put("mchid", properties.getMchId());
            payload.put("description", "商城订单 " + order.orderSn());
            payload.put("out_trade_no", paySn);
            payload.put("time_expire", order.expireTime()
                    .atZone(ZoneId.of("Asia/Shanghai")).toOffsetDateTime().toString());
            payload.put("notify_url", properties.getNotifyUrl());
            payload.put("amount", amount);
            payload.put("payer", payer);
            Map<String, Object> responseBody = postJson(JSAPI_PATH, payload);
            String prepayId = String.valueOf(responseBody.getOrDefault("prepay_id", ""));
            if (prepayId.isBlank()) {
                throw new ServerException(502, "微信支付未返回预支付标识");
            }
            return buildMiniProgramParameters(prepayId);
        } catch (ServerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServerException(502, "微信支付服务暂时不可用");
        }
    }

    public PaymentNotification parseNotification(
            String timestamp, String nonce, String signature, String serial, String body) {
        validateConfiguration();
        verifySignature(timestamp + "\n" + nonce + "\n" + body + "\n", signature, serial);
        long callbackTime;
        try {
            callbackTime = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            throw new ServerException(400, "微信支付回调时间戳无效");
        }
        if (Math.abs(System.currentTimeMillis() / 1000 - callbackTime) > 300) {
            throw new ServerException(400, "微信支付回调已过期");
        }
        try {
            Map<String, Object> envelope = objectMapper.readValue(body, new TypeReference<>() {});
            String notificationId = requireText(envelope, "id");
            String eventType = requireText(envelope, "event_type");
            if (!"TRANSACTION.SUCCESS".equals(eventType)) {
                throw new ServerException(400, "微信支付通知事件不受支持");
            }
            Map<String, Object> resource = castMap(envelope.get("resource"));
            String plaintext = decryptResource(resource);
            Map<String, Object> transaction = objectMapper.readValue(plaintext, new TypeReference<>() {});
            Map<String, Object> amount = castMap(transaction.get("amount"));
            String appId = requireText(transaction, "appid");
            String mchId = requireText(transaction, "mchid");
            String currency = requireText(amount, "currency");
            if (!properties.getAppId().equals(appId) || !properties.getMchId().equals(mchId)) {
                throw new ServerException(400, "微信支付回调商户信息不匹配");
            }
            if (!"CNY".equals(currency)) {
                throw new ServerException(400, "微信支付回调币种不匹配");
            }
            return new PaymentNotification(
                    notificationId,
                    eventType,
                    requireText(transaction, "out_trade_no"),
                    requireText(transaction, "transaction_id"),
                    requireText(transaction, "trade_state"),
                    requireAmount(amount),
                    parseSuccessTime(transaction.get("success_time"))
            );
        } catch (ServerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServerException(400, "微信支付回调内容无效");
        }
    }

    private Map<String, Object> buildMiniProgramParameters(String prepayId) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String packageValue = "prepay_id=" + prepayId;
        String message = properties.getAppId() + "\n" + timestamp + "\n" + nonce + "\n"
                + packageValue + "\n";
        return Map.of(
                "mockPay", false,
                "timeStamp", timestamp,
                "nonceStr", nonce,
                "package", packageValue,
                "signType", "RSA",
                "paySign", sign(message)
        );
    }

    private String buildAuthorization(String method, String canonicalUrl, String timestamp,
                                      String nonce, String body) throws Exception {
        String message = method + "\n" + canonicalUrl + "\n" + timestamp + "\n" + nonce
                + "\n" + body + "\n";
        return "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + properties.getMchId() + "\","
                + "nonce_str=\"" + nonce + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + properties.getMerchantSerialNo() + "\","
                + "signature=\"" + sign(message) + "\"";
    }

    private void verifyResponse(HttpResponse<String> response) {
        String timestamp = response.headers().firstValue("Wechatpay-Timestamp").orElse("");
        String nonce = response.headers().firstValue("Wechatpay-Nonce").orElse("");
        String signature = response.headers().firstValue("Wechatpay-Signature").orElse("");
        String serial = response.headers().firstValue("Wechatpay-Serial").orElse("");
        if (timestamp.isBlank() || nonce.isBlank() || signature.isBlank() || serial.isBlank()) {
            throw new ServerException(502, "微信支付响应缺少签名");
        }
        verifySignature(timestamp + "\n" + nonce + "\n" + response.body() + "\n", signature, serial);
    }

    private void verifySignature(String message, String encodedSignature, String serial) {
        try {
            X509Certificate certificate = loadPlatformCertificate();
            String certificateSerial = certificate.getSerialNumber().toString(16).toUpperCase();
            if (!certificateSerial.equalsIgnoreCase(serial)) {
                throw new ServerException(400, "微信支付平台证书序列号不匹配");
            }
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(certificate.getPublicKey());
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(encodedSignature))) {
                throw new ServerException(400, "微信支付签名验证失败");
            }
        } catch (ServerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServerException(500, "微信支付平台证书不可用");
        }
    }

    private String decryptResource(Map<String, Object> resource) throws Exception {
        String algorithm = requireText(resource, "algorithm");
        if (!"AEAD_AES_256_GCM".equals(algorithm)) {
            throw new ServerException(400, "微信支付回调加密算法不受支持");
        }
        String nonce = requireText(resource, "nonce");
        String ciphertext = requireText(resource, "ciphertext");
        byte[] key = properties.getApiV3Key().getBytes(StandardCharsets.UTF_8);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        Object associatedData = resource.get("associated_data");
        if (associatedData != null) {
            cipher.updateAAD(String.valueOf(associatedData).getBytes(StandardCharsets.UTF_8));
        }
        return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
    }

    private String sign(String message) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(loadPrivateKey());
        signer.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String pem = Files.readString(Path.of(properties.getPrivateKeyPath()), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
    }

    private X509Certificate loadPlatformCertificate() throws Exception {
        try (var input = Files.newInputStream(Path.of(properties.getPlatformCertificatePath()))) {
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(input);
        }
    }

    public void validateConfiguration() {
        if (!properties.isEnabled()
                || isBlank(properties.getAppId())
                || isBlank(properties.getMchId())
                || isBlank(properties.getMerchantSerialNo())
                || isBlank(properties.getPrivateKeyPath())
                || isBlank(properties.getApiV3Key())
                || properties.getApiV3Key().getBytes(StandardCharsets.UTF_8).length != 32
                || isBlank(properties.getPlatformCertificatePath())
                || isBlank(properties.getNotifyUrl())
                || !properties.getNotifyUrl().startsWith("https://")) {
            throw new ServerException(503, "微信支付尚未完成生产配置");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            throw new ServerException(400, "微信支付回调字段缺失");
        }
        return (Map<String, Object>) value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String requireText(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ServerException(400, "微信支付回调字段缺失: " + key);
        }
        return String.valueOf(value);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String objectToJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private int requireAmount(Map<String, Object> amount) {
        Object total = amount.get("total");
        if (!(total instanceof Number number) || number.intValue() <= 0) {
            throw new ServerException(400, "微信支付回调金额无效");
        }
        return number.intValue();
    }

    private java.time.LocalDateTime parseSuccessTime(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ServerException(400, "微信支付回调成功时间缺失");
        }
        try {
            return OffsetDateTime.parse(String.valueOf(value))
                    .atZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                    .toLocalDateTime();
        } catch (Exception exception) {
            throw new ServerException(400, "微信支付回调成功时间无效");
        }
    }

    public PaymentQueryResult queryPayment(String paySn) {
        validateConfiguration();
        try {
            String path = "/v3/pay/transactions/out-trade-no/" + paySn
                    + "?mchid=" + properties.getMchId();
            Map<String, Object> response = getJson(path);
            Map<String, Object> amount = castMap(response.get("amount"));
            String appId = requireText(response, "appid");
            String mchId = requireText(response, "mchid");
            String currency = requireText(amount, "currency");
            if (!properties.getAppId().equals(appId) || !properties.getMchId().equals(mchId)) {
                throw new ServerException(502, "微信支付查单商户信息不匹配");
            }
            if (!"CNY".equals(currency)) {
                throw new ServerException(502, "微信支付查单币种不匹配");
            }
            String tradeState = requireText(response, "trade_state");
            LocalDateTime successTime = "SUCCESS".equals(tradeState)
                    ? parseSuccessTime(response.get("success_time")) : null;
            return new PaymentQueryResult(
                    requireText(response, "out_trade_no"),
                    String.valueOf(response.getOrDefault("transaction_id", "")),
                    tradeState,
                    requireAmount(amount),
                    successTime,
                    objectMapper.writeValueAsString(response));
        } catch (ServerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServerException(502, "微信支付查单服务暂时不可用");
        }
    }

    public BillDownloadResult getTradeBillDownloadUrl(LocalDate billDate) {
        validateConfiguration();
        Map<String, Object> response = getJson("/v3/bill/tradebill?bill_date=" + billDate + "&bill_type=ALL");
        return new BillDownloadResult(text(response.get("download_url")), objectToJson(response));
    }

    public BillDownloadResult getFundFlowBillDownloadUrl(LocalDate billDate) {
        validateConfiguration();
        Map<String, Object> response = getJson("/v3/bill/fundflowbill?bill_date=" + billDate
                + "&account_type=BASIC");
        return new BillDownloadResult(text(response.get("download_url")), objectToJson(response));
    }

    public void closePayment(String paySn) {
        requestJson("POST", "/v3/pay/transactions/out-trade-no/" + paySn + "/close",
                Map.of("mchid", properties.getMchId()), Set.of("ORDER_CLOSED", "ORDERCLOSED"));
    }

    public Map<String, Object> getJson(String canonicalUrl) {
        return requestJson("GET", canonicalUrl, null);
    }

    public Map<String, Object> postJson(String canonicalUrl, Map<String, Object> payload) {
        return requestJson("POST", canonicalUrl, payload);
    }

    private Map<String, Object> requestJson(
            String method, String canonicalUrl, Map<String, Object> payload) {
        return requestJson(method, canonicalUrl, payload, Set.of());
    }

    private Map<String, Object> requestJson(
            String method, String canonicalUrl, Map<String, Object> payload,
            Set<String> acceptedErrorCodes) {
        validateConfiguration();
        try {
            String body = payload == null ? "" : objectMapper.writeValueAsString(payload);
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonce = UUID.randomUUID().toString().replace("-", "");
            String authorization = buildAuthorization(method, canonicalUrl, timestamp, nonce, body);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(API_BASE_URL + canonicalUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", authorization)
                    .header("Accept", "application/json");
            if ("POST".equals(method)) {
                builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            } else {
                builder.GET();
            }
            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            verifyResponse(response);
            Map<String, Object> responseBody = response.body() == null || response.body().isBlank()
                    ? Map.of()
                    : objectMapper.readValue(response.body(), new TypeReference<>() {});
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorCode = String.valueOf(responseBody.getOrDefault("code", ""));
                if (acceptedErrorCodes.contains(errorCode)) {
                    return responseBody;
                }
                throw new ServerException(502, "微信支付接口请求失败");
            }
            return responseBody;
        } catch (ServerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServerException(502, "微信支付服务暂时不可用");
        }
    }

    public void validateCredentialFiles() {
        validateConfiguration();
        try {
            loadPrivateKey();
            loadPlatformCertificate();
        } catch (Exception exception) {
            throw new ServerException(503, "微信支付私钥或平台证书不可用");
        }
    }

    public record TradeOrderDOView(
            Long userId, String orderSn, Integer amount, java.time.LocalDateTime expireTime) {
    }

    public record PaymentNotification(
            String notificationId, String eventType, String paySn, String transactionId,
            String tradeState, Integer amount, java.time.LocalDateTime successTime) {
    }

    public record PaymentQueryResult(
            String paySn, String transactionId, String tradeState, Integer amount,
            java.time.LocalDateTime successTime, String rawBody) {
    }

    public record BillDownloadResult(String downloadUrl, String rawBody) {
    }
}
