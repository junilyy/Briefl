package com.briefl.domain.analysis.dto;

import java.util.List;

public record AiAnalysisResult(
        String stockName,
        String briefSummary,
        String overallSentiment,
        Double newsImpactScore,
        PriceImpactAnalysis priceImpact,
        List<SentimentAnalysis> sentimentAnalyses,
        List<CheckEventAnalysis> checkEvents,
        String caution
) {
}
