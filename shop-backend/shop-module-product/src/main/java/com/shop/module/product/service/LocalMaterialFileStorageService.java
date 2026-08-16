package com.shop.module.product.service;

import com.shop.common.exception.ServerException;
import com.shop.module.product.config.MaterialStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class LocalMaterialFileStorageService implements MaterialFileStorageService {

    private final MaterialStorageProperties properties;

    @Override
    public String store(byte[] content, String objectKey) {
        Path root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new ServerException(400, "素材路径非法");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return normalizeBaseUrl(properties.getPublicBaseUrl()) + objectKey.replace("\\", "/");
        } catch (IOException e) {
            throw new ServerException(500, "素材文件保存失败");
        }
    }

    @Override
    public void delete(String objectKey) {
        Path root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new ServerException(400, "素材路径非法");
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new ServerException(500, "素材文件删除失败");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ServerException(500, "素材访问域名未配置");
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}
