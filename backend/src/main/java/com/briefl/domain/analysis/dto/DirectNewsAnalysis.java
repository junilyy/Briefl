package com.briefl.domain.analysis.dto;

public record DirectNewsAnalysis(
        String title,
        String sentiment,
        Integer sentimentScore,
        Double relevance,
        Double importance,
        Double recency,
        String reason,
        String impactLevel
) {
}
