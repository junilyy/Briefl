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
        ReportSentimentCountsResponse counts,
        List<ReportDirectNewsResponse> directNews,
        List<ReportIndirectNewsResponse> indirectNews,
        List<ReportCheckEventResponse> checkEvents,
        String caution
) {
}
