package com.briefl.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.briefl.domain.analysis.dto.AiAnalysisResult;
import com.briefl.domain.news.dto.NewsItemDto;
import com.briefl.domain.news.dto.NewsSearchResult;
import com.briefl.domain.news.dto.NewsType;
import com.briefl.global.config.OpenAiProperties;
import com.briefl.global.exception.AiAnalysisException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class OpenAiAnalysisServiceTest {

    @Test
    void analyzeReturnsMockResultWithoutCallingOpenAiWhenMockMode() {
        WebClient.Builder failingWebClientBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new AssertionError("OpenAI API must not be called in mock mode.")));
        OpenAiAnalysisService service = new OpenAiAnalysisService(
                new OpenAiProperties("", "mock", "gpt-5.4-nano", "https://api.openai.com/v1/responses", 2000),
                failingWebClientBuilder
        );

        AiAnalysisResult result = service.analyze("삼성전자", sampleNewsSearchResult());

        assertThat(result.stockName()).isEqualTo("삼성전자");
        assertThat(result.overallSentiment()).isEqualTo("중립");
        assertThat(result.directNews()).hasSize(1);
        assertThat(result.indirectNews()).hasSize(1);
    }

    @Test
    void analyzeThrowsWhenOpenAiModeHasNoApiKey() {
        OpenAiAnalysisService service = new OpenAiAnalysisService(
                new OpenAiProperties("", "openai", "gpt-5.4-nano", "https://api.openai.com/v1/responses", 2000),
                WebClient.builder()
        );

        assertThatThrownBy(() -> service.analyze("삼성전자", sampleNewsSearchResult()))
                .isInstanceOf(AiAnalysisException.class)
                .hasMessage("외부 API 인증 정보가 설정되지 않았습니다.");
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
