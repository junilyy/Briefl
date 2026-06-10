package com.briefl.domain.news.dto;

import java.util.List;

public record NaverNewsSearchResponse(
        List<NaverNewsItem> items
) {
}
