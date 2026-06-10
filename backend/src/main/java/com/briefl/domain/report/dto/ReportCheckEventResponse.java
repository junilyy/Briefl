package com.briefl.domain.report.dto;

import com.briefl.domain.analysis.dto.CheckEventAnalysis;

public record ReportCheckEventResponse(
        String date,
        String event,
        String whyImportant
) {

    public static ReportCheckEventResponse from(CheckEventAnalysis checkEvent) {
        return new ReportCheckEventResponse(
                checkEvent.date(),
                checkEvent.event(),
                checkEvent.whyImportant()
        );
    }
}
