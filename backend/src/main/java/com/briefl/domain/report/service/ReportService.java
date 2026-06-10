package com.briefl.domain.report.service;

import com.briefl.domain.analysis.dto.AiAnalysisResult;
import com.briefl.domain.analysis.dto.CheckEventAnalysis;
import com.briefl.domain.analysis.dto.ImpactScoredAnalysisResult;
import com.briefl.domain.analysis.dto.ScoredDirectNewsAnalysis;
import com.briefl.domain.analysis.dto.ScoredIndirectNewsAnalysis;
import com.briefl.domain.analysis.service.ImpactScoreService;
import com.briefl.domain.analysis.service.OpenAiAnalysisService;
import com.briefl.domain.news.dto.NewsItemDto;
import com.briefl.domain.news.dto.NewsSearchResult;
import com.briefl.domain.news.service.NewsSearchService;
import com.briefl.domain.report.dto.ReportCheckEventResponse;
import com.briefl.domain.report.dto.ReportDirectNewsResponse;
import com.briefl.domain.report.dto.ReportIndirectNewsResponse;
import com.briefl.domain.report.dto.ReportPriceImpactResponse;
import com.briefl.domain.report.dto.ReportResponse;
import com.briefl.domain.report.dto.ReportSentimentCountsResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final StockService stockService;
    private final NewsSearchService newsSearchService;
    private final OpenAiAnalysisService openAiAnalysisService;
    private final ImpactScoreService impactScoreService;
    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Transactional
    public ReportResponse createReport(String stockName) {
        Stock stock = stockService.getSupportedStock(stockName);
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        return reportRepository.findByStockNameAndReportDate(stock.getStockName(), today)
                .map(this::toReportResponse)
                .orElseGet(() -> generateAndSaveReport(stock, today));
    }

    @Transactional(readOnly = true)
    public ReportResponse getTodayReport(String stockName) {
        Stock stock = stockService.getSupportedStock(stockName);
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        return reportRepository.findByStockNameAndReportDate(stock.getStockName(), today)
                .map(this::toReportResponse)
                .orElseThrow(ReportNotFoundException::new);
    }

    private ReportResponse generateAndSaveReport(Stock stock, LocalDate reportDate) {
        NewsSearchResult newsSearchResult = newsSearchService.searchTodayNews(stock);
        AiAnalysisResult aiAnalysisResult = openAiAnalysisService.analyze(stock.getStockName(), newsSearchResult);
        ImpactScoredAnalysisResult scoredResult = impactScoreService.calculate(aiAnalysisResult);
        ReportResponse responseWithoutId = toReportResponse(null, reportDate, scoredResult, newsSearchResult);

        Report savedReport = reportRepository.save(Report.builder()
                .stockName(stock.getStockName())
                .reportDate(reportDate)
                .briefSummary(scoredResult.briefSummary())
                .overallSentiment(scoredResult.overallSentiment())
                .newsImpactScore(scoredResult.newsImpactScore())
                .priceDirection(scoredResult.priceImpact().direction())
                .priceConfidence(scoredResult.priceImpact().confidence())
                .priceReason(scoredResult.priceImpact().reason())
                .rawNewsJson(writeJson(newsSearchResult))
                .aiResultJson(writeJson(aiAnalysisResult))
                .finalResultJson(writeJson(responseWithoutId))
                .build());

        return toReportResponse(savedReport.getId(), reportDate, scoredResult, newsSearchResult);
    }

    private ReportResponse toReportResponse(Report report) {
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
                    parsed.counts(),
                    parsed.directNews(),
                    parsed.indirectNews(),
                    parsed.checkEvents(),
                    parsed.caution()
            );
        } catch (JsonProcessingException exception) {
            throw new BrieflException(ErrorCode.INTERNAL_SERVER_ERROR, "저장된 리포트 응답을 해석하지 못했습니다.");
        }
    }

    private ReportResponse toReportResponse(
            Long reportId,
            LocalDate reportDate,
            ImpactScoredAnalysisResult scoredResult,
            NewsSearchResult newsSearchResult
    ) {
        Map<String, NewsItemDto> directNewsByTitle = toNewsMap(newsSearchResult.directNews());
        Map<String, NewsItemDto> indirectNewsByTitle = toNewsMap(newsSearchResult.indirectNews());

        return new ReportResponse(
                reportId,
                scoredResult.stockName(),
                reportDate,
                scoredResult.briefSummary(),
                scoredResult.overallSentiment(),
                scoredResult.newsImpactScore(),
                ReportPriceImpactResponse.from(scoredResult.priceImpact()),
                ReportSentimentCountsResponse.from(scoredResult.counts()),
                scoredResult.directNews().stream()
                        .map(news -> toDirectNewsResponse(news, directNewsByTitle.get(normalizeTitle(news.title()))))
                        .toList(),
                scoredResult.indirectNews().stream()
                        .map(news -> toIndirectNewsResponse(news, indirectNewsByTitle.get(normalizeTitle(news.title()))))
                        .toList(),
                emptyIfNull(scoredResult.checkEvents()).stream()
                        .map(this::toCheckEventResponse)
                        .toList(),
                scoredResult.caution()
        );
    }

    private ReportDirectNewsResponse toDirectNewsResponse(ScoredDirectNewsAnalysis analysis, NewsItemDto news) {
        return new ReportDirectNewsResponse(
                analysis.title(),
                news == null ? "" : news.url(),
                news == null ? "" : news.source(),
                news == null ? null : news.publishedAt(),
                analysis.sentiment(),
                analysis.sentimentScore(),
                analysis.relevance(),
                analysis.importance(),
                analysis.recency(),
                analysis.impactScore(),
                analysis.reason()
        );
    }

    private ReportIndirectNewsResponse toIndirectNewsResponse(ScoredIndirectNewsAnalysis analysis, NewsItemDto news) {
        return new ReportIndirectNewsResponse(
                analysis.title(),
                news == null ? "" : news.url(),
                news == null ? "" : news.source(),
                news == null ? null : news.publishedAt(),
                analysis.relatedFactor(),
                analysis.sentiment(),
                analysis.sentimentScore(),
                analysis.relevance(),
                analysis.importance(),
                analysis.recency(),
                analysis.impactScore(),
                analysis.reason()
        );
    }

    private ReportCheckEventResponse toCheckEventResponse(CheckEventAnalysis checkEvent) {
        return ReportCheckEventResponse.from(checkEvent);
    }

    private Map<String, NewsItemDto> toNewsMap(List<NewsItemDto> newsItems) {
        return emptyIfNull(newsItems).stream()
                .collect(Collectors.toMap(
                        news -> normalizeTitle(news.title()),
                        Function.identity(),
                        (first, second) -> first
                ));
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.replaceAll("\\s+", " ").trim();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BrieflException(ErrorCode.INTERNAL_SERVER_ERROR, "리포트 JSON 저장 데이터 생성에 실패했습니다.");
        }
    }
}
