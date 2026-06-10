package com.briefl.domain.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "stocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_name", nullable = false, unique = true, length = 100)
    private String stockName;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 50)
    private String market;

    @Column(name = "direct_keywords_json", nullable = false, columnDefinition = "TEXT")
    private String directKeywordsJson;

    @Column(name = "indirect_keywords_json", nullable = false, columnDefinition = "TEXT")
    private String indirectKeywordsJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Stock(
            String stockName,
            String displayName,
            String market,
            String directKeywordsJson,
            String indirectKeywordsJson
    ) {
        this.stockName = stockName;
        this.displayName = displayName;
        this.market = market;
        this.directKeywordsJson = directKeywordsJson;
        this.indirectKeywordsJson = indirectKeywordsJson;
    }
}
