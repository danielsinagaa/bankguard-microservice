package com.bankguard.transaction.controller;

import com.bankguard.common.api.PageResponse;
import com.bankguard.transaction.dto.response.DailyFraudTrendReportResponse;
import com.bankguard.transaction.dto.response.DailyTopRiskyCustomerReportResponse;
import com.bankguard.transaction.dto.response.HighRiskTransactionReportResponse;
import com.bankguard.transaction.dto.response.RiskScoreDistributionReportResponse;
import com.bankguard.transaction.dto.response.SuspiciousDestinationReportResponse;
import com.bankguard.transaction.dto.response.TopRiskCustomerReportResponse;
import com.bankguard.transaction.dto.response.TransactionVelocityReportResponse;
import com.bankguard.transaction.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/high-risk-transactions")
    public PageResponse<HighRiskTransactionReportResponse> highRiskTransactions(
            @RequestParam(required = false) Integer minimumRiskScore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.highRiskTransactions(minimumRiskScore, startDate, endDate, page, size);
    }

    @GetMapping("/transaction-velocity")
    public PageResponse<TransactionVelocityReportResponse> transactionVelocity(
            @RequestParam(required = false) Integer minimumCount,
            @RequestParam(required = false) BigDecimal minimumAmount,
            @RequestParam(required = false) Integer windowMinutes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.transactionVelocity(minimumCount, minimumAmount, windowMinutes, page, size);
    }

    @GetMapping("/top-risk-customers")
    public PageResponse<TopRiskCustomerReportResponse> topRiskCustomers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minimumAverageRiskScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.topRiskCustomers(startDate, endDate, minimumAverageRiskScore, page, size);
    }

    @GetMapping("/suspicious-destination-accounts")
    public PageResponse<SuspiciousDestinationReportResponse> suspiciousDestinationAccounts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer minimumUniqueSenders,
            @RequestParam(required = false) BigDecimal minimumTotalAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.suspiciousDestinationAccounts(
                startDate,
                endDate,
                minimumUniqueSenders,
                minimumTotalAmount,
                page,
                size
        );
    }

    @GetMapping("/daily-fraud-trend")
    public PageResponse<DailyFraudTrendReportResponse> dailyFraudTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return reportService.dailyFraudTrend(startDate, endDate);
    }

    @GetMapping("/risk-score-distribution")
    public PageResponse<RiskScoreDistributionReportResponse> riskScoreDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return reportService.riskScoreDistribution(startDate, endDate);
    }

    @GetMapping("/daily-top-risky-customers")
    public PageResponse<DailyTopRiskyCustomerReportResponse> dailyTopRiskyCustomers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer topN
    ) {
        return reportService.dailyTopRiskyCustomers(startDate, endDate, topN);
    }
}
