package com.briefl.domain.stock.dto;

import com.briefl.domain.stock.entity.Stock;

public record StockResponse(
        String stockName,
        String displayName,
        String market
) {

    public static StockResponse from(Stock stock) {
        return new StockResponse(
                stock.getStockName(),
                stock.getDisplayName(),
                stock.getMarket()
        );
    }
}
