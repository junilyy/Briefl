package com.briefl.domain.analysis.dto;

public record IndirectNewsAnalysis(
        String title,
        String relatedFactor,
        String sentiment,
        Integer sentimentScore,
        Double relevance,
        Double importance,
        Double recency,
        String reason,
        String impactLevel
) {
}
