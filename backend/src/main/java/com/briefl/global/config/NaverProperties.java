package com.briefl.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "naver")
public record NaverProperties(
        String clientId,
        String clientSecret,
        String newsSearchUrl
) {
}
