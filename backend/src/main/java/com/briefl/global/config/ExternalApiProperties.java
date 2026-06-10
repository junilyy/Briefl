package com.briefl.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-api")
public record ExternalApiProperties(
        Timeout timeout
) {

    public Duration connectTimeout() {
        return Duration.ofMillis(timeout.connectMillis());
    }

    public Duration responseTimeout() {
        return Duration.ofSeconds(timeout.responseSeconds());
    }

    public Duration naverRequestTimeout() {
        return Duration.ofSeconds(timeout.naverRequestSeconds());
    }

    public Duration openAiRequestTimeout() {
        return Duration.ofSeconds(timeout.openAiRequestSeconds());
    }

    public record Timeout(
            Integer connectMillis,
            Integer responseSeconds,
            Integer naverRequestSeconds,
            Integer openAiRequestSeconds
    ) {
    }
}
