package com.briefl.domain.analysis.dto;

import java.util.List;

public record ImpactScoredAnalysisResult(
        String stockName,
        String briefSummary,
        String overallSentiment,
        Double newsImpactScore,
        PriceImpactAnalysis priceImpact,
        SentimentCounts counts,
        List<ScoredDirectNewsAnalysis> directNews,
        List<ScoredIndirectNewsAnalysis> indirectNews,
        List<CheckEventAnalysis> checkEvents,
        String caution
) {
}
