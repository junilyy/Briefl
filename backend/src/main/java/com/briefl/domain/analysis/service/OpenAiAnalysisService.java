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

    private static final String CAUTION = "";

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
                "현재는 mock 모드라 실제 뉴스 판단은 하지 않습니다. 운영 모드에서는 오늘 뉴스에서 가장 강한 긍정 요인과 부담 요인을 먼저 보여줍니다.",
                "방향 불명확",
                0.0,
                new PriceImpactAnalysis(
                        "방향 불명확",
                        "낮음",
                        "mock 모드에서는 실제 뉴스 의미를 판단하지 않으므로 가격 방향을 정하지 않습니다."
                ),
                List.of(
                        new SentimentAnalysis(
                                "호재",
                                "운영 모드에서는 매출, 수요, 정책, 수급처럼 긍정으로 읽히는 재료만 따로 묶습니다.",
                                List.of("긍정 재료가 있으면 첫 줄에서 바로 결론을 보여줍니다."),
                                referencedTitles
                        ),
                        new SentimentAnalysis(
                                "중립",
                                "방향을 정하기 어려운 뉴스는 확인 포인트로만 남깁니다.",
                                List.of("실적, 환율, 정책처럼 해석이 갈리는 항목을 분리합니다."),
                                referencedTitles
                        ),
                        new SentimentAnalysis(
                                "부담 요인",
                                "운영 모드에서는 하락 부담이나 변동성 확대 요인을 따로 묶습니다.",
                                List.of("경쟁, 비용, 수요 둔화처럼 조심해야 할 재료를 먼저 보여줍니다."),
                                referencedTitles
                        )
                ),
                List.of(new CheckEventAnalysis(null, "OpenAI 실제 분석 모드 전환", "운영 모드에서는 이벤트 후보 검색 결과까지 함께 보고 체크할 일정을 뽑습니다.")),
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
                너는 개인 투자자를 위한 주식 뉴스 분석 리포트를 작성하는 애널리스트다.
                사용자는 주식을 잘 모를 수 있으므로 결론을 먼저, 짧고 직관적으로 설명한다.
                뉴스에 근거한 방향 판단과 주관적 해석은 허용한다.
                단, 특정 매수/매도 실행 지시와 확정적 가격 예측은 작성하지 않는다.
                애매하면 애매하다고 끝내지 말고, 왜 애매한지와 무엇을 봐야 하는지를 말한다.
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
                - 기사 하나하나를 평가하지 말고, 호재 / 중립 / 부담 요인별 종합 분석을 작성
                - 각 감정 그룹은 2문장 이내로 짧게 작성하고, 첫 문장에 결론을 둔다
                - keyPoints는 사용자가 실제로 봐야 할 체크 포인트만 1~2개 작성한다
                - relatedNewsTitles에는 해당 종합 분석에 실제로 참고한 뉴스 제목만 넣는다. 모든 뉴스를 반복해서 넣지 않는다
                - briefSummary는 "오늘 결론 1문장 + 가장 중요한 이유 1문장"으로 작성한다
                - priceImpact.reason은 긍정 요인, 부담 요인, 오늘 우선 확인할 포인트를 한 문단으로 작성한다
                - "혼조"라는 단어는 쓰지 말고 "긍정 요인 우세", "부담 요인 우세", "방향 불명확", "변동성 주의" 중 하나로 표현한다
                - newsImpactScore는 오늘 뉴스 방향을 -1.0부터 1.0 사이 숫자로 작성한다
                  * 0.6 이상: 긍정 요인 우세
                  * 0.2 이상 0.6 미만: 긍정 요인 약간 우세
                  * -0.2 초과 0.2 미만: 방향 불명확
                  * -0.6 초과 -0.2 이하: 부담 요인 약간 우세
                  * -0.6 이하: 부담 요인 우세
                - checkEvents는 이벤트 후보 뉴스까지 참고해 작성한다
                - checkEvents는 실적 발표, IR, 컨퍼런스, 정책 일정, 섹터 이벤트처럼 앞으로 확인할 일을 작성한다
                - checkEvents 날짜를 뉴스에서 확인할 수 없으면 null로 둔다

                직접 뉴스:
                %s

                간접 영향 뉴스:
                %s

                이벤트 후보 검색 결과:
                %s

                응답은 반드시 아래 JSON 형식과 같은 필드명으로만 작성해라.
                {
                  "stockName": "",
                  "briefSummary": "",
                  "overallSentiment": "긍정 요인 우세 | 긍정 요인 약간 우세 | 방향 불명확 | 부담 요인 약간 우세 | 부담 요인 우세 | 변동성 주의",
                  "newsImpactScore": 0.0,
                  "priceImpact": {
                    "direction": "긍정 요인 우세 | 방향 불명확 | 부담 요인 우세 | 변동성 주의",
                    "confidence": "낮음 | 중간 | 높음",
                    "reason": ""
                  },
                  "sentimentAnalyses": [
                    {
                      "sentiment": "호재 | 중립 | 부담 요인",
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
                formatNews(newsSearchResult.eventNews()),
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
                java.util.stream.Stream.concat(
                        newsSearchResult.indirectNews().stream(),
                        newsSearchResult.eventNews().stream()
                )
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
