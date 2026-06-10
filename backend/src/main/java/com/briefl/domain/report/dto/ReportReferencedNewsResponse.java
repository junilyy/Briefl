package com.briefl.domain.report.dto;

import com.briefl.domain.news.dto.NewsItemDto;
import java.time.LocalDateTime;

public record ReportReferencedNewsResponse(
        String title,
        String url,
        String source,
        LocalDateTime publishedAt,
        String newsType
) {

    public static ReportReferencedNewsResponse from(NewsItemDto news) {
        return new ReportReferencedNewsResponse(
                news.title(),
                news.url(),
                news.source(),
                news.publishedAt(),
                news.newsType().name()
        );
    }
}
