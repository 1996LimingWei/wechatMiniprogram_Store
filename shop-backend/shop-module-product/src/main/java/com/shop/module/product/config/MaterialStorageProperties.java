package com.shop.module.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "material.storage")
public class MaterialStorageProperties {

    private String provider = "local";
    private String root = "./.runtime-uploads/material";
    private String publicBaseUrl = "http://127.0.0.1:8085/uploads/material/";
    private long maxSize = 5 * 1024 * 1024L;
    private List<String> allowedUrlPrefixes = new ArrayList<>();
}
