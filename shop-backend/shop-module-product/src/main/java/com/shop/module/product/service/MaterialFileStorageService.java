package com.shop.module.product.service;

public interface MaterialFileStorageService {

    String store(byte[] content, String objectKey);

    void delete(String objectKey);
}
