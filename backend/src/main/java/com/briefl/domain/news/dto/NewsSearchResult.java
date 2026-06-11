package com.briefl.domain.news.dto;

import java.util.List;

public record NewsSearchResult(
        List<NewsItemDto> directNews,
        List<NewsItemDto> indirectNews,
        List<NewsItemDto> eventNews
) {
}
