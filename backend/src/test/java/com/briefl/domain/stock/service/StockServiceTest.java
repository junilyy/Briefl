package com.briefl.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.briefl.domain.stock.entity.Stock;
import com.briefl.domain.stock.repository.StockRepository;
import com.briefl.global.exception.BrieflException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    void getReportStockReturnsRegisteredStockWhenNameOrDisplayNameMatches() {
        Stock registeredStock = Stock.builder()
                .stockName("네이버")
                .displayName("NAVER")
                .market("KOSPI")
                .directKeywordsJson("[\"네이버\"]")
                .indirectKeywordsJson("[\"플랫폼 규제\"]")
                .build();
        given(stockRepository.findByStockNameIgnoreCaseOrDisplayNameIgnoreCase("NAVER", "NAVER"))
                .willReturn(Optional.of(registeredStock));

        Stock result = stockService.getReportStock(" NAVER ");

        assertThat(result).isSameAs(registeredStock);
    }

    @Test
    void getReportStockCreatesAdHocStockForUnregisteredName() {
        given(stockRepository.findByStockNameIgnoreCaseOrDisplayNameIgnoreCase("현대차", "현대차"))
                .willReturn(Optional.empty());

        Stock result = stockService.getReportStock("현대차");

        assertThat(result.getStockName()).isEqualTo("현대차");
        assertThat(result.getDisplayName()).isEqualTo("현대차");
        assertThat(result.getMarket()).isEmpty();
        assertThat(result.getDirectKeywordsJson()).isEqualTo("[\"현대차\",\"현대차 실적\",\"현대차 주가\"]");
        assertThat(result.getIndirectKeywordsJson()).isEqualTo("[]");
    }

    @Test
    void getReportStockThrowsWhenNameIsBlank() {
        assertThatThrownBy(() -> stockService.getReportStock("   "))
                .isInstanceOf(BrieflException.class)
                .hasMessage("종목명을 입력해주세요.");
    }
}
