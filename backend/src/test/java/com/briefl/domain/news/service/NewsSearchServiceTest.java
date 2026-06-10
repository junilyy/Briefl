package com.briefl.domain.news.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.briefl.domain.news.dto.NewsSearchResult;
import com.briefl.domain.stock.entity.Stock;
import com.briefl.global.config.ExternalApiProperties;
import com.briefl.global.config.NaverProperties;
import com.briefl.global.exception.ErrorCode;
import com.briefl.global.exception.ExternalApiException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsSearchServiceTest {

    @Test
    void searchTodayNewsFiltersDirectNewsAndRemovesDuplicates() {
        String today = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String responseBody = """
                {
                  "items": [
                    {
                      "title": "<b>삼성전자</b> AI 반도체 수요 확대",
                      "originallink": "https://example.com/direct",
                      "link": "https://n.news.naver.com/direct",
                      "description": "삼성전자 실적 기대",
                      "pubDate": "%s"
                    },
                    {
                      "title": "반도체 업황 개선",
                      "originallink": "https://example.com/indirect",
                      "link": "https://n.news.naver.com/indirect",
                      "description": "업황 개선 기대",
                      "pubDate": "%s"
                    },
                    {
                      "title": "<b>삼성전자</b> AI 반도체 수요 확대",
                      "originallink": "https://example.com/duplicate",
                      "link": "https://n.news.naver.com/duplicate",
                      "description": "중복 기사",
                      "pubDate": "%s"
                    }
                  ]
                }
                """.formatted(today, today, today);

        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(responseBody)
                        .build()));
        NewsSearchService newsSearchService = new NewsSearchService(
                new NaverProperties("client-id", "client-secret", "https://openapi.naver.com/v1/search/news.json"),
                externalApiProperties(),
                webClientBuilder
        );

        NewsSearchResult result = newsSearchService.searchTodayNews(Stock.builder()
                .stockName("삼성전자")
                .displayName("삼성전자")
                .market("KOSPI")
                .directKeywordsJson("[\"삼성전자\"]")
                .indirectKeywordsJson("[\"반도체 업황\"]")
                .build());

        assertThat(result.directNews())
                .hasSize(1)
                .first()
                .extracting("title")
                .isEqualTo("삼성전자 AI 반도체 수요 확대");
        assertThat(result.indirectNews())
                .extracting("title")
                .contains("반도체 업황 개선")
                .doesNotContain("삼성전자 AI 반도체 수요 확대");
    }

    @Test
    void searchTodayNewsThrowsWhenNaverCredentialsAreMissing() {
        NewsSearchService newsSearchService = new NewsSearchService(
                new NaverProperties("", "", "https://openapi.naver.com/v1/search/news.json"),
                externalApiProperties(),
                WebClient.builder()
        );

        assertThatThrownBy(() -> newsSearchService.searchTodayNews(sampleStock()))
                .isInstanceOf(ExternalApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXTERNAL_API_AUTH_MISSING);
    }

    private ExternalApiProperties externalApiProperties() {
        return new ExternalApiProperties(new ExternalApiProperties.Timeout(3000, 10, 10, 30));
    }

    private Stock sampleStock() {
        return Stock.builder()
                .stockName("삼성전자")
                .displayName("삼성전자")
                .market("KOSPI")
                .directKeywordsJson("[\"삼성전자\"]")
                .indirectKeywordsJson("[\"반도체 업황\"]")
                .build();
    }
}
