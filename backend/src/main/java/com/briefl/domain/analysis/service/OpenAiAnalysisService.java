package com.briefl.domain.analysis.service;

import com.briefl.domain.analysis.dto.AiAnalysisResult;
import com.briefl.domain.analysis.dto.CheckEventAnalysis;
import com.briefl.domain.analysis.dto.DirectNewsAnalysis;
import com.briefl.domain.analysis.dto.IndirectNewsAnalysis;
import com.briefl.domain.analysis.dto.PriceImpactAnalysis;
import com.briefl.domain.news.dto.NewsItemDto;
import com.briefl.domain.news.dto.NewsSearchResult;
import com.briefl.global.config.ExternalApiProperties;
import com.briefl.global.config.OpenAiProperties;
import com.briefl.global.exception.AiAnalysisException;
import com.briefl.global.exception.ErrorCode;
import com.briefl.global.exception.ExternalApiExceptionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiAnalysisService {

    private static final String CAUTION = "본 리포트는 투자 판단을 보조하기 위한 정보 정리이며, 매수·매도 추천이 아닙니다.";

    private final OpenAiProperties openAiProperties;
    private final ExternalApiProperties externalApiProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAnalysisResult analyze(String stockName, NewsSearchResult newsSearchResult) {
        if (openAiProperties.isMockMode()) {
            return createMockAnalysis(stockName, newsSearchResult);
        }

        validateOpenAiApiKey();
        String responseBody = callOpenAi(stockName, newsSearchResult);
        return parseAnalysisResult(responseBody);
    }

    private AiAnalysisResult createMockAnalysis(String stockName, NewsSearchResult newsSearchResult) {
        List<DirectNewsAnalysis> directNews = newsSearchResult.directNews().stream()
                .map(news -> new DirectNewsAnalysis(
                        news.title(),
                        "중립",
                        0,
                        1.0,
                        0.7,
                        1.0,
                        "개발용 mock 분석입니다. 실제 OpenAI 호출 없이 뉴스와 종목의 관련성만 확인합니다.",
                        "중간"
                ))
                .toList();
        List<IndirectNewsAnalysis> indirectNews = newsSearchResult.indirectNews().stream()
                .map(news -> new IndirectNewsAnalysis(
                        news.title(),
                        "기타",
                        "중립",
                        0,
                        0.5,
                        0.7,
                        1.0,
                        "개발용 mock 분석입니다. 실제 OpenAI 호출 없이 간접 영향 뉴스 흐름만 확인합니다.",
                        "중간"
                ))
                .toList();

        return new AiAnalysisResult(
                stockName,
                "개발용 mock 분석 결과입니다. 실제 가격 영향 가능성 판단은 OpenAI 실제 호출 모드에서 생성됩니다.",
                "중립",
                new PriceImpactAnalysis(
                        "판단 어려움",
                        "낮음",
                        "현재는 비용 방지를 위한 mock 모드이므로 실제 뉴스의 투자적 의미를 판단하지 않습니다."
                ),
                directNews,
                indirectNews,
                List.of(new CheckEventAnalysis(null, "실제 OpenAI 분석 모드 전환", "AI 분석 품질을 확인하려면 AI_ANALYSIS_MODE=openai로 전환해야 합니다.")),
                CAUTION
        );
    }

    private void validateOpenAiApiKey() {
        if (!StringUtils.hasText(openAiProperties.apiKey())) {
            throw new AiAnalysisException(ErrorCode.EXTERNAL_API_AUTH_MISSING);
        }
    }

    private String callOpenAi(String stockName, NewsSearchResult newsSearchResult) {
        try {
            return webClientBuilder.build()
                    .post()
                    .uri(openAiProperties.responsesUrl())
                    .header("Authorization", "Bearer " + openAiProperties.apiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(createRequestBody(stockName, newsSearchResult))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        log.warn("OpenAI API error response: {}", body);
                                        return new AiAnalysisException(ErrorCode.EXTERNAL_API_ERROR, "OpenAI API 호출에 실패했습니다.");
                                    }))
                    .bodyToMono(String.class)
                    .block(externalApiProperties.openAiRequestTimeout());
        } catch (WebClientRequestException exception) {
            if (ExternalApiExceptionUtils.isTimeout(exception)) {
                throw new AiAnalysisException(ErrorCode.EXTERNAL_API_TIMEOUT, "OpenAI API 응답 시간이 초과되었습니다.");
            }
            throw new AiAnalysisException(ErrorCode.EXTERNAL_API_ERROR, "OpenAI API 연결에 실패했습니다.");
        } catch (RuntimeException exception) {
            if (exception instanceof AiAnalysisException aiAnalysisException) {
                throw aiAnalysisException;
            }
            if (ExternalApiExceptionUtils.isTimeout(exception)) {
                throw new AiAnalysisException(ErrorCode.EXTERNAL_API_TIMEOUT, "OpenAI API 응답 시간이 초과되었습니다.");
            }
            throw new AiAnalysisException(ErrorCode.EXTERNAL_API_ERROR, "OpenAI API 호출 중 오류가 발생했습니다.");
        }
    }

    private Map<String, Object> createRequestBody(String stockName, NewsSearchResult newsSearchResult) {
        return Map.of(
                "model", openAiProperties.model(),
                "max_output_tokens", openAiProperties.maxOutputTokens(),
                "text", Map.of(
                        "format", Map.of("type", "json_object")
                ),
                "input", List.of(
                        Map.of(
                                "role", "system",
                                "content", createSystemPrompt()
                        ),
                        Map.of(
                                "role", "user",
                                "content", createUserPrompt(stockName, newsSearchResult)
                        )
                )
        );
    }

    private String createSystemPrompt() {
        return """
                너는 개인 투자자를 위한 주식 뉴스 분석 리포트 생성 도우미다.
                투자 추천, 매수 추천, 매도 추천, 확정적 가격 예측을 절대 하지 않는다.
                "가격 예측"이라는 표현 대신 "가격 영향 가능성"이라고 표현한다.
                응답은 반드시 JSON 객체 하나로만 작성한다.
                """;
    }

    private String createUserPrompt(String stockName, NewsSearchResult newsSearchResult) {
        return """
                관심 종목: %s

                아래 뉴스들은 두 종류로 나뉜다.
                1. 직접 뉴스: 해당 종목과 직접 관련된 뉴스
                2. 간접 영향 뉴스: 종목명이 직접 등장하지 않지만 산업, 금리, 환율, 전쟁, 정책, 경쟁사 이슈처럼 해당 종목 주가에 영향을 줄 수 있는 뉴스

                각 뉴스를 분석해 다음 기준으로 정리해라.
                - 호재 / 중립 / 악재 가능성 중 하나로 분류
                - 왜 그렇게 판단했는지 근거 작성
                - 가격 변동에 영향을 줄 수 있는 요인인지 판단
                - 단, 주가를 단정적으로 예측하지 말고 "영향 가능성"으로 표현
                - 투자 추천, 매수/매도 지시는 하지 말 것

                sentimentScore 기준:
                - 호재: 1
                - 중립: 0
                - 악재 가능성: -1

                relevance 기준:
                - 직접 관련 뉴스: 1.0
                - 산업 관련 뉴스: 0.7
                - 매크로/사회 이슈: 0.5
                - 약한 관련 이슈: 0.3

                importance 기준:
                - 높음: 1.0
                - 중간: 0.7
                - 낮음: 0.4

                recency 기준:
                - 오늘 뉴스: 1.0
                - 1~2일 전 뉴스: 0.8
                - 3~5일 전 뉴스: 0.5
                - 일주일 이상: 0.2

                직접 뉴스:
                %s

                간접 영향 뉴스:
                %s

                응답은 반드시 아래 JSON 형식과 같은 필드명으로만 작성해라.
                {
                  "stockName": "",
                  "briefSummary": "",
                  "overallSentiment": "호재 우세 | 중립 | 악재 리스크 존재 | 혼조",
                  "priceImpact": {
                    "direction": "상승 요인 우세 | 하락 리스크 존재 | 혼조 | 판단 어려움",
                    "confidence": "낮음 | 중간 | 높음",
                    "reason": ""
                  },
                  "directNews": [
                    {
                      "title": "",
                      "sentiment": "호재 | 중립 | 악재 가능성",
                      "sentimentScore": 1,
                      "relevance": 1.0,
                      "importance": 0.7,
                      "recency": 1.0,
                      "reason": "",
                      "impactLevel": "낮음 | 중간 | 높음"
                    }
                  ],
                  "indirectNews": [
                    {
                      "title": "",
                      "relatedFactor": "금리 | 환율 | 전쟁 | 유가 | 산업 | 경쟁사 | 정책 | 기타",
                      "sentiment": "호재 | 중립 | 악재 가능성",
                      "sentimentScore": 0,
                      "relevance": 0.5,
                      "importance": 0.7,
                      "recency": 1.0,
                      "reason": "",
                      "impactLevel": "낮음 | 중간 | 높음"
                    }
                  ],
                  "checkEvents": [
                    {
                      "date": "",
                      "event": "",
                      "whyImportant": ""
                    }
                  ],
                  "caution": "%s"
                }
                """.formatted(
                stockName,
                formatNews(newsSearchResult.directNews()),
                formatNews(newsSearchResult.indirectNews()),
                CAUTION
        );
    }

    private String formatNews(List<NewsItemDto> newsItems) {
        if (newsItems.isEmpty()) {
            return "- 수집된 뉴스 없음";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < newsItems.size(); i++) {
            NewsItemDto news = newsItems.get(i);
            builder.append(i + 1)
                    .append(". 제목: ").append(news.title()).append("\n")
                    .append("   요약: ").append(news.description()).append("\n")
                    .append("   출처: ").append(news.source()).append("\n")
                    .append("   URL: ").append(news.url()).append("\n")
                    .append("   발행일: ").append(news.publishedAt()).append("\n");
        }
        return builder.toString();
    }

    private AiAnalysisResult parseAnalysisResult(String responseBody) {
        try {
            String outputText = extractOutputText(responseBody);
            return objectMapper.readValue(outputText, AiAnalysisResult.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            log.warn("Failed to parse OpenAI analysis response. raw={}", responseBody, exception);
            throw new AiAnalysisException(ErrorCode.AI_ANALYSIS_PARSE_ERROR);
        }
    }

    private String extractOutputText(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode outputItem : output) {
                JsonNode content = outputItem.path("content");
                if (content.isArray()) {
                    for (JsonNode contentItem : content) {
                        JsonNode text = contentItem.path("text");
                        if (text.isTextual()) {
                            return text.asText();
                        }
                    }
                }
            }
        }

        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual()) {
            return outputText.asText();
        }

        throw new IllegalArgumentException("OpenAI response output text is empty.");
    }
}
