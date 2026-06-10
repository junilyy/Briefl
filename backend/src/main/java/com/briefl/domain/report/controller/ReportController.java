package com.briefl.domain.report.controller;

import com.briefl.domain.report.dto.ReportRequest;
import com.briefl.domain.report.dto.ReportResponse;
import com.briefl.domain.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "리포트 생성",
            description = """
                    오늘 날짜의 종목 리포트가 있으면 캐시를 반환하고, 없으면 뉴스 수집과 AI 분석 후 저장합니다.
                    AI_ANALYSIS_MODE=mock이면 OpenAI API 비용 없이 mock 분석 결과를 생성합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리포트 생성 또는 캐시 반환 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류 또는 지원하지 않는 종목",
                    content = @Content(schema = @Schema(implementation = com.briefl.global.apiPayload.ApiResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "외부 API 인증 정보 누락",
                    content = @Content(schema = @Schema(implementation = com.briefl.global.apiPayload.ApiResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "외부 API 호출 실패 또는 AI 응답 파싱 실패",
                    content = @Content(schema = @Schema(implementation = com.briefl.global.apiPayload.ApiResponse.class))
            ),
            @ApiResponse(
                    responseCode = "504",
                    description = "외부 API 응답 시간 초과",
                    content = @Content(schema = @Schema(implementation = com.briefl.global.apiPayload.ApiResponse.class))
            )
    })
    @PostMapping
    public ReportResponse createReport(@Valid @RequestBody ReportRequest request) {
        return reportService.createReport(request.stockName());
    }

    @Operation(summary = "오늘 리포트 조회", description = "오늘 날짜에 이미 생성된 종목 리포트를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "오늘 리포트 조회 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 종목",
                    content = @Content(schema = @Schema(implementation = com.briefl.global.apiPayload.ApiResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "오늘 생성된 리포트 없음",
                    content = @Content(schema = @Schema(implementation = com.briefl.global.apiPayload.ApiResponse.class))
            )
    })
    @GetMapping
    public ReportResponse getTodayReport(
            @Parameter(description = "지원 종목명", example = "삼성전자")
            @RequestParam String stockName
    ) {
        return reportService.getTodayReport(stockName);
    }
}
