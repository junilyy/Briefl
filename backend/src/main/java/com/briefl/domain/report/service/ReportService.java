package com.briefl.domain.report.service;

import com.briefl.domain.analysis.dto.AiAnalysisResult;
import com.briefl.domain.analysis.dto.CheckEventAnalysis;
import com.briefl.domain.analysis.service.OpenAiAnalysisService;
import com.briefl.domain.news.dto.NewsItemDto;
import com.briefl.domain.news.dto.NewsSearchResult;
import com.briefl.domain.news.service.NewsSearchService;
import com.briefl.domain.report.dto.ReportCheckEventResponse;
import com.briefl.domain.report.dto.ReportPriceImpactResponse;
import com.briefl.domain.report.dto.ReportReferencedNewsResponse;
import com.briefl.domain.report.dto.ReportResponse;
import com.briefl.domain.report.dto.ReportSentimentAnalysisResponse;
import com.briefl.domain.report.entity.Report;
import com.briefl.domain.report.exception.ReportNotFoundException;
import com.briefl.domain.report.repository.ReportRepository;
import com.briefl.domain.stock.entity.Stock;
import com.briefl.domain.stock.service.StockService;
import com.briefl.global.exception.BrieflException;
import com.briefl.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final StockService stockService;
    private final NewsSearchService newsSearchService;
    private final OpenAiAnalysisService openAiAnalysisService;
    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Transactional
    public ReportResponse createReport(String stockName) {
        Stock stock = stockService.getSupportedStock(stockName);
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        return reportRepository.findByStockNameAndReportDate(stock.getStockName(), today)
                .map(report -> toReportResponseOrRegenerate(report, stock, today))
                .orElseGet(() -> generateAndSaveReport(stock, today));
    }

    @Transactional
    public ReportResponse getTodayReport(String stockName) {
        Stock stock = stockService.getSupportedStock(stockName);
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        return reportRepository.findByStockNameAndReportDate(stock.getStockName(), today)
                .map(report -> toReportResponseOrRegenerate(report, stock, today))
                .orElseThrow(ReportNotFoundException::new);
    }

    private ReportResponse generateAndSaveReport(Stock stock, LocalDate reportDate) {
        NewsSearchResult newsSearchResult = newsSearchService.searchTodayNews(stock);
        AiAnalysisResult aiAnalysisResult = openAiAnalysisService.analyze(stock.getStockName(), newsSearchResult);
        ReportResponse responseWithoutId = toReportResponse(null, reportDate, aiAnalysisResult, newsSearchResult);

        Report savedReport = reportRepository.save(Report.builder()
                .stockName(stock.getStockName())
                .reportDate(reportDate)
                .briefSummary(aiAnalysisResult.briefSummary())
                .overallSentiment(aiAnalysisResult.overallSentiment())
                .newsImpactScore(defaultScore(aiAnalysisResult.newsImpactScore()))
                .priceDirection(aiAnalysisResult.priceImpact().direction())
                .priceConfidence(aiAnalysisResult.priceImpact().confidence())
                .priceReason(aiAnalysisResult.priceImpact().reason())
                .rawNewsJson(writeJson(newsSearchResult))
                .aiResultJson(writeJson(aiAnalysisResult))
                .finalResultJson(writeJson(responseWithoutId))
                .build());

        return toReportResponse(savedReport.getId(), reportDate, aiAnalysisResult, newsSearchResult);
    }

    private ReportResponse toReportResponseOrRegenerate(Report report, Stock stock, LocalDate reportDate) {
        ReportResponse response = toReportResponseOrNull(report);
        if (response != null) {
            return response;
        }

        log.info("Regenerating report because saved finalResultJson is incompatible. reportId={}", report.getId());
        reportRepository.delete(report);
        reportRepository.flush();
        return generateAndSaveReport(stock, reportDate);
    }

    private ReportResponse toReportResponseOrNull(Report report) {
        try {
            ReportResponse parsed = objectMapper.readValue(report.getFinalResultJson(), ReportResponse.class);
            return new ReportResponse(
                    report.getId(),
                    parsed.stockName(),
                    parsed.reportDate(),
                    parsed.briefSummary(),
                    parsed.overallSentiment(),
                    parsed.newsImpactScore(),
                    parsed.priceImpact(),
                    parsed.referencedNews(),
                    parsed.sentimentAnalyses(),
                    parsed.checkEvents(),
                    parsed.caution()
            );
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private ReportResponse toReportResponse(
            Long reportId,
            LocalDate reportDate,
            AiAnalysisResult aiAnalysisResult,
            NewsSearchResult newsSearchResult
    ) {
        return new ReportResponse(
                reportId,
                aiAnalysisResult.stockName(),
                reportDate,
                aiAnalysisResult.briefSummary(),
                aiAnalysisResult.overallSentiment(),
                defaultScore(aiAnalysisResult.newsImpactScore()),
                ReportPriceImpactResponse.from(aiAnalysisResult.priceImpact()),
                referencedNews(newsSearchResult).stream()
                        .map(ReportReferencedNewsResponse::from)
                        .toList(),
                emptyIfNull(aiAnalysisResult.sentimentAnalyses()).stream()
                        .map(ReportSentimentAnalysisResponse::from)
                        .toList(),
                emptyIfNull(aiAnalysisResult.checkEvents()).stream()
                        .map(this::toCheckEventResponse)
                        .toList(),
                aiAnalysisResult.caution()
        );
    }

    private ReportCheckEventResponse toCheckEventResponse(CheckEventAnalysis checkEvent) {
        return ReportCheckEventResponse.from(checkEvent);
    }

    private List<NewsItemDto> referencedNews(NewsSearchResult newsSearchResult) {
        List<NewsItemDto> referencedNews = new ArrayList<>();
        referencedNews.addAll(emptyIfNull(newsSearchResult.directNews()));
        referencedNews.addAll(emptyIfNull(newsSearchResult.indirectNews()));
        return referencedNews;
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private double defaultScore(Double score) {
        return score == null ? 0.0 : score;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BrieflException(ErrorCode.INTERNAL_SERVER_ERROR, "리포트 JSON 저장 데이터 생성에 실패했습니다.");
        }
    }
}
