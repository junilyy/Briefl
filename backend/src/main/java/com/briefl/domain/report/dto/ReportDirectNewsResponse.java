package com.briefl.domain.report.dto;

import java.time.LocalDateTime;

public record ReportDirectNewsResponse(
        String title,
        String url,
        String source,
        LocalDateTime publishedAt,
        String sentiment,
        Integer sentimentScore,
        Double relevance,
        Double importance,
        Double recency,
        Double impactScore,
        String reason
) {
}
