package com.briefl.domain.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "brief_summary", nullable = false, columnDefinition = "TEXT")
    private String briefSummary;

    @Column(name = "overall_sentiment", nullable = false, length = 50)
    private String overallSentiment;

    @Column(name = "news_impact_score", nullable = false)
    private Double newsImpactScore;

    @Column(name = "price_direction", nullable = false, length = 50)
    private String priceDirection;

    @Column(name = "price_confidence", nullable = false, length = 50)
    private String priceConfidence;

    @Column(name = "price_reason", nullable = false, columnDefinition = "TEXT")
    private String priceReason;

    @Column(name = "raw_news_json", nullable = false, columnDefinition = "TEXT")
    private String rawNewsJson;

    @Column(name = "ai_result_json", nullable = false, columnDefinition = "TEXT")
    private String aiResultJson;

    @Column(name = "final_result_json", nullable = false, columnDefinition = "TEXT")
    private String finalResultJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Report(
            String stockName,
            LocalDate reportDate,
            String briefSummary,
            String overallSentiment,
            Double newsImpactScore,
            String priceDirection,
            String priceConfidence,
            String priceReason,
            String rawNewsJson,
            String aiResultJson,
            String finalResultJson
    ) {
        this.stockName = stockName;
        this.reportDate = reportDate;
        this.briefSummary = briefSummary;
        this.overallSentiment = overallSentiment;
        this.newsImpactScore = newsImpactScore;
        this.priceDirection = priceDirection;
        this.priceConfidence = priceConfidence;
        this.priceReason = priceReason;
        this.rawNewsJson = rawNewsJson;
        this.aiResultJson = aiResultJson;
        this.finalResultJson = finalResultJson;
    }
}
