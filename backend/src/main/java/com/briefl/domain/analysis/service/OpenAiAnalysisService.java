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
                .map(news -> createDirectNewsAnalysis(stockName, news))
                .toList();
        List<IndirectNewsAnalysis> indirectNews = newsSearchResult.indirectNews().stream()
                .map(news -> createIndirectNewsAnalysis(stockName, news))
                .toList();
        int impactScore = directNews.stream().mapToInt(DirectNewsAnalysis::sentimentScore).sum()
                + indirectNews.stream().mapToInt(IndirectNewsAnalysis::sentimentScore).sum();
        String overallSentiment = resolveOverallSentiment(impactScore);
        String direction = resolveDirection(impactScore);
        String confidence = resolveConfidence(directNews.size() + indirectNews.size(), impactScore);

        return new AiAnalysisResult(
                stockName,
                createBriefSummary(stockName, directNews, indirectNews, direction),
                overallSentiment,
                new PriceImpactAnalysis(
                        direction,
                        confidence,
                        createPriceImpactReason(stockName, directNews, indirectNews, direction)
                ),
                directNews,
                indirectNews,
                createCheckEvents(stockName, indirectNews),
                CAUTION
        );
    }

    private DirectNewsAnalysis createDirectNewsAnalysis(String stockName, NewsItemDto news) {
        MockNewsSignal signal = analyzeNewsSignal(news);
        return new DirectNewsAnalysis(
                news.title(),
                signal.sentiment(),
                signal.sentimentScore(),
                1.0,
                signal.importance(),
                1.0,
                createDirectReason(stockName, news, signal),
                signal.impactLevel()
        );
    }

    private IndirectNewsAnalysis createIndirectNewsAnalysis(String stockName, NewsItemDto news) {
        MockNewsSignal signal = analyzeNewsSignal(news);
        String relatedFactor = resolveRelatedFactor(news);
        double relevance = resolveIndirectRelevance(relatedFactor);

        return new IndirectNewsAnalysis(
                news.title(),
                relatedFactor,
                signal.sentiment(),
                signal.sentimentScore(),
                relevance,
                signal.importance(),
                1.0,
                createIndirectReason(stockName, news, signal, relatedFactor),
                signal.impactLevel()
        );
    }

    private MockNewsSignal analyzeNewsSignal(NewsItemDto news) {
        String text = normalize(news.title() + " " + news.description());
        int positiveMatches = countMatches(text, List.of(
                "호실적", "실적 개선", "흑자", "수요 증가", "수요 확대", "성장", "수주", "공급 계약",
                "투자", "증설", "출시", "승인", "강세", "상승", "급등", "기대", "개선", "회복", "확대",
                "AI", "HBM", "반도체 수요", "금리 인하", "환율 안정", "수출 증가"
        ));
        int negativeMatches = countMatches(text, List.of(
                "적자", "감소", "부진", "둔화", "하락", "급락", "약세", "우려", "리스크", "소송",
                "파업", "분쟁", "규제", "압박", "제재", "감원", "실패", "경고", "악화", "금리 인상",
                "환율 급등", "유가 급등", "전쟁", "관세"
        ));
        double importance = resolveImportance(text);

        if (positiveMatches > negativeMatches) {
            return new MockNewsSignal("호재", 1, importance, resolveImpactLevel(importance));
        }
        if (negativeMatches > positiveMatches) {
            return new MockNewsSignal("악재 가능성", -1, importance, resolveImpactLevel(importance));
        }
        return new MockNewsSignal("중립", 0, importance, resolveImpactLevel(importance));
    }

    private double resolveImportance(String text) {
        if (containsAny(text, List.of("실적", "수주", "공급 계약", "투자", "규제", "금리", "환율", "유가", "전쟁", "소송", "파업", "AI", "HBM", "반도체"))) {
            return 1.0;
        }
        if (containsAny(text, List.of("출시", "협력", "정책", "경쟁", "수요", "수출", "시장", "업황"))) {
            return 0.7;
        }
        return 0.4;
    }

    private String resolveImpactLevel(double importance) {
        if (importance >= 1.0) {
            return "높음";
        }
        if (importance >= 0.7) {
            return "중간";
        }
        return "낮음";
    }

    private String resolveRelatedFactor(NewsItemDto news) {
        String text = normalize(news.title() + " " + news.description());
        if (containsAny(text, List.of("금리", "연준", "FOMC", "채권"))) {
            return "금리";
        }
        if (containsAny(text, List.of("환율", "원달러", "달러", "엔화"))) {
            return "환율";
        }
        if (containsAny(text, List.of("전쟁", "분쟁", "중동", "우크라이나"))) {
            return "전쟁";
        }
        if (containsAny(text, List.of("유가", "원유", "석유", "OPEC"))) {
            return "유가";
        }
        if (containsAny(text, List.of("TSMC", "엔비디아", "애플", "구글", "마이크로소프트", "경쟁사"))) {
            return "경쟁사";
        }
        if (containsAny(text, List.of("정책", "규제", "정부", "법안", "관세", "제재"))) {
            return "정책";
        }
        if (containsAny(text, List.of("반도체", "AI", "플랫폼", "배터리", "전기차", "광고", "커머스", "업황", "수출"))) {
            return "산업";
        }
        return "기타";
    }

    private double resolveIndirectRelevance(String relatedFactor) {
        return switch (relatedFactor) {
            case "산업", "경쟁사" -> 0.7;
            case "금리", "환율", "전쟁", "유가", "정책" -> 0.5;
            default -> 0.3;
        };
    }

    private String createDirectReason(String stockName, NewsItemDto news, MockNewsSignal signal) {
        return switch (signal.sentiment()) {
            case "호재" -> "%s와 직접 연결된 이슈로, '%s' 보도는 실적 기대나 투자 심리에 긍정적으로 작용할 가능성이 있습니다."
                    .formatted(stockName, news.title());
            case "악재 가능성" -> "%s와 직접 연결된 이슈로, '%s' 보도는 비용 부담이나 투자 심리 위축 요인으로 해석될 수 있습니다."
                    .formatted(stockName, news.title());
            default -> "%s 관련 보도이지만 현재 제목과 요약만으로는 뚜렷한 방향성을 단정하기 어려워 중립으로 분류했습니다."
                    .formatted(stockName);
        };
    }

    private String createIndirectReason(String stockName, NewsItemDto news, MockNewsSignal signal, String relatedFactor) {
        return switch (signal.sentiment()) {
            case "호재" -> "%s 요인은 %s의 업황과 투자 심리에 간접적으로 우호적인 영향을 줄 가능성이 있습니다."
                    .formatted(relatedFactor, stockName);
            case "악재 가능성" -> "%s 요인은 %s의 수요, 비용, 밸류에이션에 부담으로 작용할 가능성이 있어 추가 확인이 필요합니다."
                    .formatted(relatedFactor, stockName);
            default -> "'%s' 보도는 %s와 관련된 간접 변수지만, 현재 정보만으로 방향성을 판단하기는 제한적입니다."
                    .formatted(news.title(), stockName);
        };
    }

    private String createBriefSummary(String stockName, List<DirectNewsAnalysis> directNews, List<IndirectNewsAnalysis> indirectNews, String direction) {
        long positive = countSentiment(directNews, indirectNews, "호재");
        long negative = countSentiment(directNews, indirectNews, "악재 가능성");
        return "오늘 %s 관련 뉴스는 호재성 이슈 %d건, 악재 가능성 이슈 %d건으로 집계됐으며, 종합적으로는 %s로 정리됩니다."
                .formatted(stockName, positive, negative, direction);
    }

    private String createPriceImpactReason(String stockName, List<DirectNewsAnalysis> directNews, List<IndirectNewsAnalysis> indirectNews, String direction) {
        String mainPositive = findFirstTitle(directNews, indirectNews, "호재");
        String mainNegative = findFirstTitle(directNews, indirectNews, "악재 가능성");
        if (StringUtils.hasText(mainPositive) && StringUtils.hasText(mainNegative)) {
            return "긍정 요인으로는 '%s'가 확인되지만, '%s'도 함께 나타나 %s의 가격 영향 가능성은 혼조로 볼 수 있습니다."
                    .formatted(mainPositive, mainNegative, stockName);
        }
        if (StringUtils.hasText(mainPositive)) {
            return "'%s'가 핵심 긍정 요인으로 확인되며, %s에 대한 투자 심리에 우호적으로 작용할 가능성이 있습니다."
                    .formatted(mainPositive, stockName);
        }
        if (StringUtils.hasText(mainNegative)) {
            return "'%s'가 주요 부담 요인으로 확인되며, %s 관련 단기 투자 심리에 부담으로 작용할 가능성이 있습니다."
                    .formatted(mainNegative, stockName);
        }
        return "수집된 뉴스에서 뚜렷한 방향성은 제한적이어서 %s의 가격 영향 가능성은 %s로 정리됩니다."
                .formatted(stockName, direction);
    }

    private List<CheckEventAnalysis> createCheckEvents(String stockName, List<IndirectNewsAnalysis> indirectNews) {
        List<String> factors = indirectNews.stream()
                .map(IndirectNewsAnalysis::relatedFactor)
                .distinct()
                .limit(2)
                .toList();
        if (factors.isEmpty()) {
            return List.of(new CheckEventAnalysis(null, stockName + " 후속 공시 및 실적 관련 뉴스", "직접 뉴스가 추가로 나올 경우 가격 영향 가능성 판단이 달라질 수 있습니다."));
        }
        return factors.stream()
                .map(factor -> new CheckEventAnalysis(null, factor + " 관련 후속 뉴스", factor + " 변화는 " + stockName + "의 투자 심리와 업황 판단에 영향을 줄 수 있습니다."))
                .toList();
    }

    private String resolveOverallSentiment(int impactScore) {
        if (impactScore > 0) {
            return "호재 우세";
        }
        if (impactScore < 0) {
            return "악재 리스크 존재";
        }
        return "혼조";
    }

    private String resolveDirection(int impactScore) {
        if (impactScore > 0) {
            return "상승 요인 우세";
        }
        if (impactScore < 0) {
            return "하락 리스크 존재";
        }
        return "혼조";
    }

    private String resolveConfidence(int newsCount, int impactScore) {
        if (newsCount >= 6 && Math.abs(impactScore) >= 2) {
            return "중간";
        }
        return "낮음";
    }

    private long countSentiment(List<DirectNewsAnalysis> directNews, List<IndirectNewsAnalysis> indirectNews, String sentiment) {
        return directNews.stream().filter(news -> sentiment.equals(news.sentiment())).count()
                + indirectNews.stream().filter(news -> sentiment.equals(news.sentiment())).count();
    }

    private String findFirstTitle(List<DirectNewsAnalysis> directNews, List<IndirectNewsAnalysis> indirectNews, String sentiment) {
        return directNews.stream()
                .filter(news -> sentiment.equals(news.sentiment()))
                .map(DirectNewsAnalysis::title)
                .findFirst()
                .orElseGet(() -> indirectNews.stream()
                        .filter(news -> sentiment.equals(news.sentiment()))
                        .map(IndirectNewsAnalysis::title)
                        .findFirst()
                        .orElse(""));
    }

    private int countMatches(String text, List<String> keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) {
                count++;
            }
        }
        return count;
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(keyword -> text.contains(normalize(keyword)));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private record MockNewsSignal(
            String sentiment,
            Integer sentimentScore,
            Double importance,
            String impactLevel
    ) {
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
