package com.briefl.domain.report.dto;

import java.time.LocalDate;
import java.util.List;

public record ReportResponse(
        Long reportId,
        String stockName,
        LocalDate reportDate,
        String briefSummary,
        String overallSentiment,
        Double newsImpactScore,
        ReportPriceImpactResponse priceImpact,
        List<ReportReferencedNewsResponse> referencedNews,
        List<ReportSentimentAnalysisResponse> sentimentAnalyses,
        List<ReportCheckEventResponse> checkEvents,
        String caution
) {
}
