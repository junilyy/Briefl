package com.briefl.domain.analysis.dto;

public record PriceImpactAnalysis(
        String direction,
        String confidence,
        String reason
) {
}
