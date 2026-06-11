package com.briefl.global.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final ExternalApiProperties externalApiProperties;

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(externalApiProperties.connectTimeout().toMillis()))
                .responseTimeout(maxTimeout(
                        externalApiProperties.responseTimeout(),
                        externalApiProperties.openAiRequestTimeout()
                ));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    private Duration maxTimeout(Duration first, Duration second) {
        return first.compareTo(second) >= 0 ? first : second;
    }
}
