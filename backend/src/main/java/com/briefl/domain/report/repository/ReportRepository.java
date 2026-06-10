package com.briefl.domain.report.repository;

import com.briefl.domain.report.entity.Report;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByStockNameAndReportDate(String stockName, LocalDate reportDate);

    boolean existsByStockNameAndReportDate(String stockName, LocalDate reportDate);
}
