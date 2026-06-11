package com.briefl.domain.analysis.service;

import com.briefl.domain.analysis.dto.AiAnalysisResult;
import com.briefl.domain.analysis.dto.CheckEventAnalysis;
import com.briefl.domain.analysis.dto.PriceImpactAnalysis;
import com.briefl.domain.analysis.dto.SentimentAnalysis;
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
        List<String> referencedTitles = referencedNews(newsSearchResult).stream()
                .map(NewsItemDto::title)
                .toList();

        return new AiAnalysisResult(
                stockName,
                "개발용 mock 분석 결과입니다. 참고 뉴스 목록과 응답 구조 확인용이며, 실제 종합 분석은 OpenAI 실제 호출 모드에서 생성됩니다.",
                "중립",
                0.0,
                new PriceImpactAnalysis(
                        "판단 어려움",
                        "낮음",
                        "현재는 비용 방지를 위한 mock 모드이므로 실제 뉴스의 투자적 의미를 판단하지 않습니다."
                ),
                List.of(
                        new SentimentAnalysis(
                                "호재",
                                "mock 모드에서는 참고 뉴스의 긍정 요인을 실제로 판별하지 않습니다.",
                                List.of("응답 구조와 참고 뉴스 링크 노출 여부를 확인합니다."),
                                referencedTitles
                        ),
                        new SentimentAnalysis(
                                "중립",
                                "mock 모드에서는 대부분의 뉴스를 중립적 확인 대상으로 둡니다.",
                                List.of("실제 종합 분석은 AI_ANALYSIS_MODE=openai에서 생성됩니다."),
                                referencedTitles
                        ),
                        new SentimentAnalysis(
                                "악재 가능성",
                                "mock 모드에서는 부정 요인을 실제로 판별하지 않습니다.",
                                List.of("운영 모드 전환 후 악재 가능성 분석 품질을 확인해야 합니다."),
                                referencedTitles
                        )
                ),
                List.of(new CheckEventAnalysis(null, "OpenAI 실제 분석 모드 전환", "AI가 참고 뉴스 기반 체크 이벤트를 생성하는지 확인해야 합니다.")),
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

                참고 뉴스 전체를 바탕으로 다음 기준으로 정리해라.
                - 기사 하나하나를 평가하지 말고, 호재 / 중립 / 악재 가능성별 종합 분석을 작성
                - 각 감정 그룹에는 여러 뉴스에서 공통적으로 읽히는 맥락과 핵심 근거를 요약
                - relatedNewsTitles에는 해당 종합 분석에 실제로 참고한 뉴스 제목만 넣기
                - checkEvents는 별도 캘린더 API가 아니라, 아래 뉴스에서 언급되었거나 뉴스 맥락상 가까운 시일 내 확인해야 할 이벤트를 AI가 도출
                - checkEvents의 날짜를 뉴스에서 확인할 수 없으면 null로 둔다
                - 단, 주가를 단정적으로 예측하지 말고 "영향 가능성"으로 표현
                - 투자 추천, 매수/매도 지시는 하지 말 것
                - newsImpactScore는 리포트 전체의 뉴스 영향 방향을 -1.0부터 1.0 사이 숫자로 작성

                직접 뉴스:
                %s

                간접 영향 뉴스:
                %s

                응답은 반드시 아래 JSON 형식과 같은 필드명으로만 작성해라.
                {
                  "stockName": "",
                  "briefSummary": "",
                  "overallSentiment": "호재 우세 | 중립 | 악재 리스크 존재 | 혼조",
                  "newsImpactScore": 0.0,
                  "priceImpact": {
                    "direction": "상승 요인 우세 | 하락 리스크 존재 | 혼조 | 판단 어려움",
                    "confidence": "낮음 | 중간 | 높음",
                    "reason": ""
                  },
                  "sentimentAnalyses": [
                    {
                      "sentiment": "호재 | 중립 | 악재 가능성",
                      "summary": "",
                      "keyPoints": ["", ""],
                      "relatedNewsTitles": ["", ""]
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

    private List<NewsItemDto> referencedNews(NewsSearchResult newsSearchResult) {
        return java.util.stream.Stream.concat(
                newsSearchResult.directNews().stream(),
                newsSearchResult.indirectNews().stream()
        ).toList();
    }

    private AiAnalysisResult parseAnalysisResult(String responseBody) {
        try {
            String outputText = extractJsonObjectText(extractOutputText(responseBody));
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

    private String extractJsonObjectText(String outputText) {
        int start = outputText.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("OpenAI response JSON object is empty.");
        }

        boolean inString = false;
        boolean escaped = false;
        int depth = 0;

        for (int i = start; i < outputText.length(); i++) {
            char current = outputText.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (current == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (current == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return outputText.substring(start, i + 1);
                }
            }
        }

        throw new IllegalArgumentException("OpenAI response JSON object is incomplete.");
    }
}
