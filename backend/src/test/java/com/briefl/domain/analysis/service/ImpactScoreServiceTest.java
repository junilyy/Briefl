package com.briefl.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.briefl.domain.analysis.dto.AiAnalysisResult;
import com.briefl.domain.analysis.dto.DirectNewsAnalysis;
import com.briefl.domain.analysis.dto.ImpactScoredAnalysisResult;
import com.briefl.domain.analysis.dto.IndirectNewsAnalysis;
import com.briefl.domain.analysis.dto.PriceImpactAnalysis;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImpactScoreServiceTest {

    private final ImpactScoreService impactScoreService = new ImpactScoreService();

    @Test
    void calculateAddsImpactScoresAndCountsSentiments() {
        AiAnalysisResult analysisResult = new AiAnalysisResult(
                "삼성전자",
                "반도체 수요 기대와 금리 부담이 함께 있습니다.",
                "혼조",
                new PriceImpactAnalysis("판단 어려움", "중간", "AI 분석 원문 사유"),
                List.of(
                        new DirectNewsAnalysis("호재 뉴스", "호재", 1, 1.0, 0.8, 1.0, "수요 확대", "높음"),
                        new DirectNewsAnalysis("중립 뉴스", "중립", 0, 1.0, 0.7, 1.0, "방향성 제한", "중간")
                ),
                List.of(
                        new IndirectNewsAnalysis("악재 뉴스", "금리", "악재 가능성", -1, 0.5, 0.7, 1.0, "금리 부담", "중간")
                ),
                List.of(),
                "본 리포트는 투자 판단을 보조하기 위한 정보 정리이며, 매수·매도 추천이 아닙니다."
        );

        ImpactScoredAnalysisResult result = impactScoreService.calculate(analysisResult);

        assertThat(result.directNews().get(0).impactScore()).isEqualTo(0.8);
        assertThat(result.directNews().get(1).impactScore()).isEqualTo(0.0);
        assertThat(result.indirectNews().get(0).impactScore()).isEqualTo(-0.35);
        assertThat(result.newsImpactScore()).isEqualTo(0.45);
        assertThat(result.counts().positive()).isEqualTo(1);
        assertThat(result.counts().neutral()).isEqualTo(1);
        assertThat(result.counts().negative()).isEqualTo(1);
        assertThat(result.overallSentiment()).isEqualTo("호재 우세");
        assertThat(result.priceImpact().direction()).isEqualTo("상승 요인 우세");
        assertThat(result.priceImpact().confidence()).isEqualTo("중간");
    }

    @Test
    void calculateClassifiesNegativeScoreAsDownsideRisk() {
        AiAnalysisResult analysisResult = new AiAnalysisResult(
                "테슬라",
                "부정적 요인이 우세합니다.",
                "혼조",
                new PriceImpactAnalysis("혼조", "낮음", "AI 분석 원문 사유"),
                List.of(new DirectNewsAnalysis("악재 직접 뉴스", "악재 가능성", -1, 1.0, 0.8, 1.0, "실적 부담", "높음")),
                List.of(),
                List.of(),
                "본 리포트는 투자 판단을 보조하기 위한 정보 정리이며, 매수·매도 추천이 아닙니다."
        );

        ImpactScoredAnalysisResult result = impactScoreService.calculate(analysisResult);

        assertThat(result.newsImpactScore()).isEqualTo(-0.8);
        assertThat(result.overallSentiment()).isEqualTo("악재 리스크 존재");
        assertThat(result.priceImpact().direction()).isEqualTo("하락 리스크 존재");
    }

    @Test
    void calculateClassifiesSmallScoreAsMixed() {
        AiAnalysisResult analysisResult = new AiAnalysisResult(
                "네이버",
                "방향성이 뚜렷하지 않습니다.",
                "중립",
                new PriceImpactAnalysis("판단 어려움", "낮음", "AI 분석 원문 사유"),
                List.of(new DirectNewsAnalysis("약한 호재 뉴스", "호재", 1, 0.3, 0.7, 1.0, "약한 관련", "낮음")),
                List.of(),
                List.of(),
                "본 리포트는 투자 판단을 보조하기 위한 정보 정리이며, 매수·매도 추천이 아닙니다."
        );

        ImpactScoredAnalysisResult result = impactScoreService.calculate(analysisResult);

        assertThat(result.newsImpactScore()).isEqualTo(0.21);
        assertThat(result.overallSentiment()).isEqualTo("혼조");
        assertThat(result.priceImpact().direction()).isEqualTo("혼조");
    }
}
