package com.briefl.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String analysisMode,
        String model,
        String responsesUrl,
        Integer maxOutputTokens
) {

    public boolean isMockMode() {
        return !"openai".equalsIgnoreCase(analysisMode);
    }
}
