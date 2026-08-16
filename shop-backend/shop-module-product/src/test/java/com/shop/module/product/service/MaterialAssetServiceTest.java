package com.shop.module.product.service;

import com.shop.common.exception.ServerException;
import com.shop.module.product.config.MaterialStorageProperties;
import com.shop.module.product.dal.dataobject.MaterialAssetDO;
import com.shop.module.product.dal.mysql.MaterialAssetMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaterialAssetServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreValidatedImageAndInsertAsset() throws Exception {
        MaterialAssetMapper mapper = mock(MaterialAssetMapper.class);
        MaterialStorageProperties properties = properties();
        LocalMaterialFileStorageService storageService = new LocalMaterialFileStorageService(properties);
        MaterialAssetService service = new MaterialAssetService(mapper, storageService, properties);
        MockMultipartFile file = new MockMultipartFile(
                "file", "product.png", "image/png", onePixelPng());

        service.upload(file, "product", 99L);

        ArgumentCaptor<MaterialAssetDO> captor = ArgumentCaptor.forClass(MaterialAssetDO.class);
        verify(mapper).insert(captor.capture());
        MaterialAssetDO asset = captor.getValue();
        assertEquals("product.png", asset.getFileName());
        assertEquals("image/png", asset.getContentType());
        assertEquals("product", asset.getBizType());
        assertEquals(99L, asset.getCreatedBy());
        assertEquals(1, asset.getWidth());
        assertEquals(1, asset.getHeight());
        assertTrue(asset.getUrl().startsWith("https://cdn.example.com/uploads/material/"));
        assertTrue(Files.exists(tempDir.resolve(asset.getObjectKey())));
    }

    @Test
    void shouldRejectFileWithInvalidMagicBytes() {
        MaterialAssetMapper mapper = mock(MaterialAssetMapper.class);
        MaterialStorageProperties properties = properties();
        MaterialAssetService service = new MaterialAssetService(
                mapper, new LocalMaterialFileStorageService(properties), properties);
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "not a png image".getBytes());

        ServerException exception = assertThrows(ServerException.class,
                () -> service.upload(file, "product", 99L));

        assertEquals(400, exception.getCode());
        verify(mapper, never()).insert(any());
    }

    @Test
    void shouldRejectWhenExtensionContentTypeAndMagicBytesMismatch() {
        MaterialAssetMapper mapper = mock(MaterialAssetMapper.class);
        MaterialStorageProperties properties = properties();
        MaterialAssetService service = new MaterialAssetService(
                mapper, new LocalMaterialFileStorageService(properties), properties);
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg", onePixelPng());

        ServerException exception = assertThrows(ServerException.class,
                () -> service.upload(file, "product", 99L));

        assertEquals(400, exception.getCode());
        verify(mapper, never()).insert(any());
    }

    @Test
    void shouldRejectDeleteWhenAssetIsReferenced() {
        MaterialAssetMapper mapper = mock(MaterialAssetMapper.class);
        MaterialFileStorageService storageService = mock(MaterialFileStorageService.class);
        MaterialAssetService service = new MaterialAssetService(mapper, storageService, properties());
        MaterialAssetDO asset = new MaterialAssetDO();
        asset.setId(10L);
        asset.setUrl("https://cdn.example.com/uploads/material/asset.png");
        asset.setObjectKey("2026/08/16/asset.png");
        asset.setReferenceCount(0);
        when(mapper.selectById(10L)).thenReturn(asset);
        when(mapper.countReferences(asset.getUrl())).thenReturn(2);

        ServerException exception = assertThrows(ServerException.class, () -> service.delete(10L));

        assertEquals(409, exception.getCode());
        verify(mapper, never()).deleteById(10L);
        verify(storageService, never()).delete(any());
    }

    private MaterialStorageProperties properties() {
        MaterialStorageProperties properties = new MaterialStorageProperties();
        properties.setProvider("local");
        properties.setRoot(tempDir.toString());
        properties.setPublicBaseUrl("https://cdn.example.com/uploads/material/");
        properties.setMaxSize(5 * 1024 * 1024L);
        return properties;
    }

    private byte[] onePixelPng() {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");
    }
}
