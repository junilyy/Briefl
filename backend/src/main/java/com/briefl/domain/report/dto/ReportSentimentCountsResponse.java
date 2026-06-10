package com.briefl.domain.report.dto;

import com.briefl.domain.analysis.dto.SentimentCounts;

public record ReportSentimentCountsResponse(
        int positive,
        int neutral,
        int negative
) {

    public static ReportSentimentCountsResponse from(SentimentCounts counts) {
        return new ReportSentimentCountsResponse(
                counts.positive(),
                counts.neutral(),
                counts.negative()
        );
    }
}
