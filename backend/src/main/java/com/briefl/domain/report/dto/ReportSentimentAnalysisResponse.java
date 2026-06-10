package com.briefl.domain.report.dto;

import com.briefl.domain.analysis.dto.SentimentAnalysis;
import java.util.List;

public record ReportSentimentAnalysisResponse(
        String sentiment,
        String summary,
        List<String> keyPoints,
        List<String> relatedNewsTitles
) {

    public static ReportSentimentAnalysisResponse from(SentimentAnalysis analysis) {
        return new ReportSentimentAnalysisResponse(
                analysis.sentiment(),
                analysis.summary(),
                analysis.keyPoints(),
                analysis.relatedNewsTitles()
        );
    }
}
