package com.briefl.domain.report.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportRequest(
        @NotBlank(message = "stockName은 필수입니다.")
        String stockName
) {
}
