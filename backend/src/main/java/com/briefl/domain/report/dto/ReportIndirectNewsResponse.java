package com.briefl.domain.report.dto;

import java.time.LocalDateTime;

public record ReportIndirectNewsResponse(
        String title,
        String url,
        String source,
        LocalDateTime publishedAt,
        String relatedFactor,
        String sentiment,
        Integer sentimentScore,
        Double relevance,
        Double importance,
        Double recency,
        Double impactScore,
        String reason
) {
}
