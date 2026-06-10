package com.briefl.domain.analysis.dto;

public record ScoredDirectNewsAnalysis(
        String title,
        String sentiment,
        Integer sentimentScore,
        Double relevance,
        Double importance,
        Double recency,
        Double impactScore,
        String reason,
        String impactLevel
) {

    public static ScoredDirectNewsAnalysis from(DirectNewsAnalysis news, double impactScore) {
        return new ScoredDirectNewsAnalysis(
                news.title(),
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
