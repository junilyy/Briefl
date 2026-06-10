package com.briefl.domain.stock.init;

import com.briefl.domain.stock.entity.Stock;
import com.briefl.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockDataInitializer implements CommandLineRunner {

    private final StockRepository stockRepository;

    @Override
    public void run(String... args) {
        saveIfNotExists(
                "삼성전자",
                "삼성전자",
                "KOSPI",
                """
                        ["삼성전자","삼성전자 실적","삼성전자 반도체","삼성전자 HBM","삼성전자 파운드리"]
                        """,
                """
                        ["반도체 업황","원달러 환율","미국 금리","미중 무역 갈등","AI 반도체 수요","TSMC","엔비디아 실적","전쟁 유가","반도체 수출"]
                        """
        );
        saveIfNotExists(
                "네이버",
                "NAVER",
                "KOSPI",
                """
                        ["네이버","네이버 실적","네이버 AI","네이버 클라우드","네이버 커머스"]
                        """,
                """
                        ["플랫폼 규제","온라인 광고 시장","AI 검색","클라우드 산업","금리","원달러 환율","카카오 실적"]
                        """
        );
        saveIfNotExists(
                "카카오",
                "카카오",
                "KOSPI",
                """
                        ["카카오","카카오 실적","카카오톡","카카오페이","카카오모빌리티"]
                        """,
                """
                        ["플랫폼 규제","온라인 광고 시장","모빌리티 정책","금리","네이버 실적","핀테크 규제"]
                        """
        );
        saveIfNotExists(
                "테슬라",
                "Tesla",
                "NASDAQ",
                """
                        ["테슬라","테슬라 실적","테슬라 전기차","테슬라 자율주행","테슬라 인도량"]
                        """,
                """
                        ["전기차 수요","미국 금리","리튬 가격","중국 전기차","전기차 보조금","일론 머스크","미국 소비 지표"]
                        """
        );
        saveIfNotExists(
                "엔비디아",
                "NVIDIA",
                "NASDAQ",
                """
                        ["엔비디아","엔비디아 실적","엔비디아 GPU","엔비디아 AI 칩","엔비디아 데이터센터"]
                        """,
                """
                        ["AI 반도체 수요","미국 금리","TSMC","반도체 수출 규제","데이터센터 투자","빅테크 AI 투자","HBM"]
                        """
        );
    }

    private void saveIfNotExists(
            String stockName,
            String displayName,
            String market,
            String directKeywordsJson,
            String indirectKeywordsJson
    ) {
        if (stockRepository.existsByStockName(stockName)) {
            return;
        }

        stockRepository.save(Stock.builder()
                .stockName(stockName)
                .displayName(displayName)
                .market(market)
                .directKeywordsJson(directKeywordsJson.strip())
                .indirectKeywordsJson(indirectKeywordsJson.strip())
                .build());
    }
}
