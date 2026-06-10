package com.briefl.domain.analysis.dto;

public record CheckEventAnalysis(
        String date,
        String event,
        String whyImportant
) {
}
