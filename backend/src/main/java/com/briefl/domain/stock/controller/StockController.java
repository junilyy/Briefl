package com.briefl.domain.stock.controller;

import com.briefl.domain.stock.dto.StockResponse;
import com.briefl.domain.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stock", description = "지원 종목 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    @Operation(summary = "지원 종목 목록 조회", description = "BRIEFL MVP에서 지원하는 관심 종목 목록을 조회합니다.")
    @GetMapping
    public List<StockResponse> getStocks() {
        return stockService.getStocks();
    }
}
