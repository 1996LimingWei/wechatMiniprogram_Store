package com.shop.module.product.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppMockControllerBoundaryTest {
    @Test
    void controllerOnlyExposesExplicitMockPaths() {
        for (Method method : AppMockController.class.getDeclaredMethods()) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping == null) continue;
            assertTrue(Arrays.stream(mapping.value()).allMatch(path -> path.startsWith("/app-api/mock/")), method.getName());
        }
    }
}
