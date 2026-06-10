package com.briefl.domain.analysis.dto;

import java.util.List;

public record SentimentAnalysis(
        String sentiment,
        String summary,
        List<String> keyPoints,
        List<String> relatedNewsTitles
) {
}
