package com.briefl.domain.news.dto;

import java.time.LocalDateTime;

public record NewsItemDto(
        String title,
        String description,
        String url,
        String source,
        LocalDateTime publishedAt,
        NewsType newsType
) {
}
