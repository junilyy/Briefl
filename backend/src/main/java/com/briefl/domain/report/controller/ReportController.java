package com.briefl.domain.report.controller;

import com.briefl.domain.report.dto.ReportRequest;
import com.briefl.domain.report.dto.ReportResponse;
import com.briefl.domain.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report", description = "뉴스 브리프 리포트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "리포트 생성", description = "오늘 날짜의 종목 리포트가 있으면 캐시를 반환하고, 없으면 뉴스 수집과 AI 분석 후 저장합니다.")
    @PostMapping
    public ReportResponse createReport(@Valid @RequestBody ReportRequest request) {
        return reportService.createReport(request.stockName());
    }

    @Operation(summary = "오늘 리포트 조회", description = "오늘 날짜에 이미 생성된 종목 리포트를 조회합니다.")
    @GetMapping
    public ReportResponse getTodayReport(@RequestParam String stockName) {
        return reportService.getTodayReport(stockName);
    }
}
