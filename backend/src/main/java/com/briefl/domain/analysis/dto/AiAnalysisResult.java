package com.briefl.domain.analysis.dto;

import java.util.List;

public record AiAnalysisResult(
        String stockName,
        String briefSummary,
        String overallSentiment,
        PriceImpactAnalysis priceImpact,
        List<DirectNewsAnalysis> directNews,
        List<IndirectNewsAnalysis> indirectNews,
        List<CheckEventAnalysis> checkEvents,
        String caution
) {
}
