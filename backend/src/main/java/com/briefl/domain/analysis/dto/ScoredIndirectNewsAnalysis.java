package com.briefl.domain.analysis.dto;

public record ScoredIndirectNewsAnalysis(
        String title,
        String relatedFactor,
        String sentiment,
        Integer sentimentScore,
        Double relevance,
        Double importance,
        Double recency,
        Double impactScore,
        String reason,
        String impactLevel
) {

    public static ScoredIndirectNewsAnalysis from(IndirectNewsAnalysis news, double impactScore) {
        return new ScoredIndirectNewsAnalysis(
                news.title(),
                news.relatedFactor(),
                news.sentiment(),
                news.sentimentScore(),
                news.relevance(),
                news.importance(),
                news.recency(),
                impactScore,
                news.reason(),
                news.impactLevel()
        );
    }
}
