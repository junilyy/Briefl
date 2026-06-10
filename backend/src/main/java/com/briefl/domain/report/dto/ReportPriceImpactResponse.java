package com.briefl.domain.report.dto;

import com.briefl.domain.analysis.dto.PriceImpactAnalysis;

public record ReportPriceImpactResponse(
        String direction,
        String confidence,
        String reason
) {

    public static ReportPriceImpactResponse from(PriceImpactAnalysis priceImpact) {
        return new ReportPriceImpactResponse(
                priceImpact.direction(),
                priceImpact.confidence(),
                priceImpact.reason()
        );
    }
}
