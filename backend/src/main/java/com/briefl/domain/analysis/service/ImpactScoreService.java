package com.briefl.domain.analysis.service;

import com.briefl.domain.analysis.dto.AiAnalysisResult;
import com.briefl.domain.analysis.dto.DirectNewsAnalysis;
import com.briefl.domain.analysis.dto.ImpactScoredAnalysisResult;
import com.briefl.domain.analysis.dto.IndirectNewsAnalysis;
import com.briefl.domain.analysis.dto.PriceImpactAnalysis;
import com.briefl.domain.analysis.dto.ScoredDirectNewsAnalysis;
import com.briefl.domain.analysis.dto.ScoredIndirectNewsAnalysis;
import com.briefl.domain.analysis.dto.SentimentCounts;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ImpactScoreService {

    private static final double POSITIVE_THRESHOLD = 0.3;
    private static final double NEGATIVE_THRESHOLD = -0.3;

    public ImpactScoredAnalysisResult calculate(AiAnalysisResult analysisResult) {
        List<ScoredDirectNewsAnalysis> directNews = emptyIfNull(analysisResult.directNews()).stream()
                .map(news -> ScoredDirectNewsAnalysis.from(news, calculateImpactScore(news)))
                .toList();
        List<ScoredIndirectNewsAnalysis> indirectNews = emptyIfNull(analysisResult.indirectNews()).stream()
                .map(news -> ScoredIndirectNewsAnalysis.from(news, calculateImpactScore(news)))
                .toList();

        double newsImpactScore = roundScore(
                directNews.stream().mapToDouble(ScoredDirectNewsAnalysis::impactScore).sum()
                        + indirectNews.stream().mapToDouble(ScoredIndirectNewsAnalysis::impactScore).sum()
        );
        SentimentCounts counts = countSentiments(analysisResult);
        PriceImpactAnalysis correctedPriceImpact = correctPriceImpact(analysisResult.priceImpact(), newsImpactScore);

        return new ImpactScoredAnalysisResult(
                analysisResult.stockName(),
                analysisResult.briefSummary(),
                correctOverallSentiment(newsImpactScore),
                newsImpactScore,
                correctedPriceImpact,
                counts,
                directNews,
                indirectNews,
                analysisResult.checkEvents(),
                analysisResult.caution()
        );
    }

    private double calculateImpactScore(DirectNewsAnalysis news) {
        return calculateImpactScore(
                news.sentimentScore(),
                news.relevance(),
                news.importance(),
                news.recency()
        );
    }

    private double calculateImpactScore(IndirectNewsAnalysis news) {
        return calculateImpactScore(
                news.sentimentScore(),
                news.relevance(),
                news.importance(),
                news.recency()
        );
    }

    private double calculateImpactScore(Integer sentimentScore, Double relevance, Double importance, Double recency) {
        return roundScore(
                defaultDouble(sentimentScore)
                        * defaultDouble(relevance)
                        * defaultDouble(importance)
                        * defaultDouble(recency)
        );
    }

    private SentimentCounts countSentiments(AiAnalysisResult analysisResult) {
        int positive = 0;
        int neutral = 0;
        int negative = 0;

        for (DirectNewsAnalysis news : emptyIfNull(analysisResult.directNews())) {
            int score = defaultDouble(news.sentimentScore()).intValue();
            if (score > 0) {
                positive++;
            } else if (score < 0) {
                negative++;
            } else {
                neutral++;
            }
        }

        for (IndirectNewsAnalysis news : emptyIfNull(analysisResult.indirectNews())) {
            int score = defaultDouble(news.sentimentScore()).intValue();
            if (score > 0) {
                positive++;
            } else if (score < 0) {
                negative++;
            } else {
                neutral++;
            }
        }

        return new SentimentCounts(positive, neutral, negative);
    }

    private PriceImpactAnalysis correctPriceImpact(PriceImpactAnalysis priceImpact, double newsImpactScore) {
        String confidence = priceImpact == null ? "낮음" : priceImpact.confidence();
        String reason = priceImpact == null ? "" : priceImpact.reason();

        return new PriceImpactAnalysis(
                directionByScore(newsImpactScore),
                confidence,
                reason
        );
    }

    private String correctOverallSentiment(double newsImpactScore) {
        if (newsImpactScore >= POSITIVE_THRESHOLD) {
            return "호재 우세";
        }
        if (newsImpactScore <= NEGATIVE_THRESHOLD) {
            return "악재 리스크 존재";
        }
        return "혼조";
    }

    private String directionByScore(double newsImpactScore) {
        if (newsImpactScore >= POSITIVE_THRESHOLD) {
            return "상승 요인 우세";
        }
        if (newsImpactScore <= NEGATIVE_THRESHOLD) {
            return "하락 리스크 존재";
        }
        return "혼조";
    }

    private Double defaultDouble(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private double roundScore(double score) {
        return Math.round(score * 10_000.0) / 10_000.0;
    }
}
