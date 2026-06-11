package com.briefl.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.briefl.domain.analysis.dto.AiAnalysisResult;
import com.briefl.domain.news.dto.NewsItemDto;
import com.briefl.domain.news.dto.NewsSearchResult;
import com.briefl.domain.news.dto.NewsType;
import com.briefl.global.config.ExternalApiProperties;
import com.briefl.global.config.OpenAiProperties;
import com.briefl.global.exception.AiAnalysisException;
import com.briefl.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class OpenAiAnalysisServiceTest {

    @Test
    void analyzeReturnsMockResultWithoutCallingOpenAiWhenMockMode() {
        WebClient.Builder failingWebClientBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new AssertionError("OpenAI API must not be called in mock mode.")));
        OpenAiAnalysisService service = new OpenAiAnalysisService(
                new OpenAiProperties("", "mock", "gpt-5.4-nano", "https://api.openai.com/v1/responses", 2000),
                externalApiProperties(),
                failingWebClientBuilder
        );

        AiAnalysisResult result = service.analyze("삼성전자", sampleNewsSearchResult());

        assertThat(result.stockName()).isEqualTo("삼성전자");
        assertThat(result.overallSentiment()).isEqualTo("중립");
        assertThat(result.newsImpactScore()).isEqualTo(0.0);
        assertThat(result.sentimentAnalyses()).hasSize(3);
        assertThat(result.sentimentAnalyses().get(0).relatedNewsTitles())
                .contains("삼성전자 AI 반도체 수요 확대", "미국 금리 동결 가능성 확대");
        assertThat(result.checkEvents()).hasSize(1);
    }

    @Test
    void analyzeThrowsWhenOpenAiModeHasNoApiKey() {
        OpenAiAnalysisService service = new OpenAiAnalysisService(
                new OpenAiProperties("", "openai", "gpt-5.4-nano", "https://api.openai.com/v1/responses", 2000),
                externalApiProperties(),
                WebClient.builder()
        );

        assertThatThrownBy(() -> service.analyze("삼성전자", sampleNewsSearchResult()))
                .isInstanceOf(AiAnalysisException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXTERNAL_API_AUTH_MISSING);
    }

    @Test
    void analyzeParsesJsonObjectEvenWhenOpenAiWrapsOutputText() {
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("""
                                {
                                  "output": [
                                    {
                                      "content": [
                                        {
                                          "type": "output_text",
                                          "text": "```json\\n{\\n  \\"stockName\\": \\"삼성전자\\",\\n  \\"briefSummary\\": \\"반도체 뉴스가 긍정 요인으로 정리됩니다.\\",\\n  \\"overallSentiment\\": \\"호재 우세\\",\\n  \\"newsImpactScore\\": 0.3,\\n  \\"priceImpact\\": {\\n    \\"direction\\": \\"상승 요인 우세\\",\\n    \\"confidence\\": \\"중간\\",\\n    \\"reason\\": \\"AI 반도체 수요 관련 뉴스가 긍정적으로 작용할 수 있습니다.\\"\\n  },\\n  \\"sentimentAnalyses\\": [\\n    {\\n      \\"sentiment\\": \\"호재\\",\\n      \\"summary\\": \\"AI 수요가 긍정적으로 언급됩니다.\\",\\n      \\"keyPoints\\": [\\"AI 반도체 수요\\"],\\n      \\"relatedNewsTitles\\": [\\"삼성전자 AI 반도체 수요 확대\\"]\\n    }\\n  ],\\n  \\"checkEvents\\": [\\n    {\\n      \\"date\\": null,\\n      \\"event\\": \\"반도체 업황 코멘트 확인\\",\\n      \\"whyImportant\\": \\"향후 수요 전망 확인이 필요합니다.\\"\\n    }\\n  ],\\n  \\"caution\\": \\"본 리포트는 투자 판단을 보조하기 위한 정보 정리이며, 매수·매도 추천이 아닙니다.\\"\\n}\\n```"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """)
                        .build()));
        OpenAiAnalysisService service = new OpenAiAnalysisService(
                new OpenAiProperties("test-key", "openai", "gpt-5.4-nano", "https://api.openai.com/v1/responses", 2000),
                externalApiProperties(),
                webClientBuilder
        );

        AiAnalysisResult result = service.analyze("삼성전자", sampleNewsSearchResult());

        assertThat(result.stockName()).isEqualTo("삼성전자");
        assertThat(result.overallSentiment()).isEqualTo("호재 우세");
        assertThat(result.priceImpact().direction()).isEqualTo("상승 요인 우세");
        assertThat(result.sentimentAnalyses()).hasSize(1);
        assertThat(result.checkEvents().get(0).date()).isNull();
    }

    private ExternalApiProperties externalApiProperties() {
        return new ExternalApiProperties(new ExternalApiProperties.Timeout(3000, 10, 10, 30));
    }

    private NewsSearchResult sampleNewsSearchResult() {
        return new NewsSearchResult(
                List.of(new NewsItemDto(
                        "삼성전자 AI 반도체 수요 확대",
                        "삼성전자 AI 반도체 수요가 확대되고 있다는 뉴스입니다.",
                        "https://example.com/direct",
                        "example.com",
                        LocalDateTime.now(),
                        NewsType.DIRECT
                )),
                List.of(new NewsItemDto(
                        "미국 금리 동결 가능성 확대",
                        "미국 금리 동결 가능성이 확대되고 있다는 뉴스입니다.",
                        "https://example.com/indirect",
                        "example.com",
                        LocalDateTime.now(),
                        NewsType.INDIRECT
                ))
        );
    }
}
