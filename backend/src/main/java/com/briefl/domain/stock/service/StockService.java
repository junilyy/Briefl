package com.briefl.domain.stock.service;

import com.briefl.domain.stock.dto.StockResponse;
import com.briefl.domain.stock.entity.Stock;
import com.briefl.domain.stock.repository.StockRepository;
import com.briefl.global.exception.BrieflException;
import com.briefl.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<StockResponse> getStocks() {
        return stockRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(StockResponse::from)
                .toList();
    }

    public Stock getReportStock(String stockName) {
        String normalizedStockName = normalizeStockName(stockName);

        return stockRepository.findByStockNameIgnoreCaseOrDisplayNameIgnoreCase(normalizedStockName, normalizedStockName)
                .orElseGet(() -> createAdHocStock(normalizedStockName));
    }

    private String normalizeStockName(String stockName) {
        if (!StringUtils.hasText(stockName)) {
            throw new BrieflException(ErrorCode.BAD_REQUEST, "종목명을 입력해주세요.");
        }

        return stockName.trim();
    }

    private Stock createAdHocStock(String stockName) {
        return Stock.builder()
                .stockName(stockName)
                .displayName(stockName)
                .market("")
                .directKeywordsJson(writeKeywords(List.of(
                        stockName,
                        stockName + " 실적",
                        stockName + " 주가"
                )))
                .indirectKeywordsJson("[]")
                .build();
    }

    private String writeKeywords(List<String> keywords) {
        try {
            return objectMapper.writeValueAsString(keywords);
        } catch (JsonProcessingException exception) {
            throw new BrieflException(ErrorCode.INTERNAL_SERVER_ERROR, "종목 검색 키워드 생성에 실패했습니다.");
        }
    }
}
