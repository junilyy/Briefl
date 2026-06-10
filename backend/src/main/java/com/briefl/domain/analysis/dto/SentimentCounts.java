package com.briefl.domain.analysis.dto;

public record SentimentCounts(
        int positive,
        int neutral,
        int negative
) {
}
