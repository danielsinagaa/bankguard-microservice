package com.bankguard.transaction.service;

import com.bankguard.common.api.PageResponse;
import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.transaction.dto.response.DailyFraudTrendReportResponse;
import com.bankguard.transaction.dto.response.DailyTopRiskyCustomerReportResponse;
import com.bankguard.transaction.dto.response.HighRiskTransactionReportResponse;
import com.bankguard.transaction.dto.response.RiskScoreDistributionReportResponse;
import com.bankguard.transaction.dto.response.SuspiciousDestinationReportResponse;
import com.bankguard.transaction.dto.response.TopRiskCustomerReportResponse;
import com.bankguard.transaction.dto.response.TransactionVelocityReportResponse;
import com.bankguard.transaction.repository.ReportRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int DEFAULT_MINIMUM_RISK_SCORE = 50;
    private static final int DEFAULT_MINIMUM_COUNT = 5;
    private static final BigDecimal DEFAULT_MINIMUM_AMOUNT = new BigDecimal("50000000");
    private static final int DEFAULT_WINDOW_MINUTES = 10;
    private static final BigDecimal DEFAULT_MINIMUM_AVERAGE_RISK_SCORE = new BigDecimal("50");
    private static final int DEFAULT_MINIMUM_UNIQUE_SENDERS = 3;
    private static final int DEFAULT_TOP_N = 10;

    private final ReportRepository reportRepository;
    private final Clock clock;

    public ReportService(ReportRepository reportRepository, Clock clock) {
        this.reportRepository = reportRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<HighRiskTransactionReportResponse> highRiskTransactions(
            Integer minimumRiskScore,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateDateRange(startDate, endDate);
        validatePage(page, size);
        int threshold = positiveOrDefault(minimumRiskScore, DEFAULT_MINIMUM_RISK_SCORE, "Minimum risk score must be positive");
        List<HighRiskTransactionReportResponse> data = reportRepository.findHighRiskTransactions(
                threshold,
                startDate,
                endDate,
                page,
                size
        );
        long total = reportRepository.countHighRiskTransactions(threshold, startDate, endDate);
        return PageResponse.of(data, page, size, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionVelocityReportResponse> transactionVelocity(
            Integer minimumCount,
            BigDecimal minimumAmount,
            Integer windowMinutes,
            int page,
            int size
    ) {
        validatePage(page, size);
        int countThreshold = positiveOrDefault(minimumCount, DEFAULT_MINIMUM_COUNT, "Minimum count must be positive");
        BigDecimal amountThreshold = positiveOrDefault(minimumAmount, DEFAULT_MINIMUM_AMOUNT, "Minimum amount must be positive");
        int window = positiveOrDefault(windowMinutes, DEFAULT_WINDOW_MINUTES, "Window minutes must be positive");
        Instant windowStart = Instant.now(clock).minusSeconds(window * 60L);
        List<TransactionVelocityReportResponse> data = reportRepository.findTransactionVelocity(
                countThreshold,
                amountThreshold,
                windowStart,
                page,
                size
        );
        long total = reportRepository.countTransactionVelocity(countThreshold, amountThreshold, windowStart);
        return PageResponse.of(data, page, size, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<TopRiskCustomerReportResponse> topRiskCustomers(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minimumAverageRiskScore,
            int page,
            int size
    ) {
        validateDateRange(startDate, endDate);
        validatePage(page, size);
        BigDecimal threshold = positiveOrDefault(
                minimumAverageRiskScore,
                DEFAULT_MINIMUM_AVERAGE_RISK_SCORE,
                "Minimum average risk score must be positive"
        );
        List<TopRiskCustomerReportResponse> data = reportRepository.findTopRiskCustomers(
                startDate,
                endDate,
                threshold,
                page,
                size
        );
        long total = reportRepository.countTopRiskCustomers(startDate, endDate, threshold);
        return PageResponse.of(data, page, size, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<SuspiciousDestinationReportResponse> suspiciousDestinationAccounts(
            LocalDate startDate,
            LocalDate endDate,
            Integer minimumUniqueSenders,
            BigDecimal minimumTotalAmount,
            int page,
            int size
    ) {
        validateDateRange(startDate, endDate);
        validatePage(page, size);
        int senderThreshold = positiveOrDefault(
                minimumUniqueSenders,
                DEFAULT_MINIMUM_UNIQUE_SENDERS,
                "Minimum unique senders must be positive"
        );
        BigDecimal amountThreshold = positiveOrDefault(
                minimumTotalAmount,
                DEFAULT_MINIMUM_AMOUNT,
                "Minimum total amount must be positive"
        );
        List<SuspiciousDestinationReportResponse> data = reportRepository.findSuspiciousDestinationAccounts(
                startDate,
                endDate,
                senderThreshold,
                amountThreshold,
                page,
                size
        );
        long total = reportRepository.countSuspiciousDestinationAccounts(startDate, endDate, senderThreshold, amountThreshold);
        return PageResponse.of(data, page, size, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<DailyFraudTrendReportResponse> dailyFraudTrend(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        List<DailyFraudTrendReportResponse> data = reportRepository.findDailyFraudTrend(startDate, endDate);
        return PageResponse.of(data, DEFAULT_PAGE, DEFAULT_SIZE, data.size());
    }

    @Transactional(readOnly = true)
    public PageResponse<RiskScoreDistributionReportResponse> riskScoreDistribution(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        List<RiskScoreDistributionReportResponse> data = reportRepository.findRiskScoreDistribution(startDate, endDate);
        return PageResponse.of(data, DEFAULT_PAGE, DEFAULT_SIZE, data.size());
    }

    @Transactional(readOnly = true)
    public PageResponse<DailyTopRiskyCustomerReportResponse> dailyTopRiskyCustomers(
            LocalDate startDate,
            LocalDate endDate,
            Integer topN
    ) {
        validateDateRange(startDate, endDate);
        int limit = positiveOrDefault(topN, DEFAULT_TOP_N, "Top N must be positive");
        if (limit > MAX_PAGE_SIZE) {
            throw new ApiException(400, ErrorCode.INVALID_PAGE_SIZE, "Top N must be between 1 and 100");
        }
        List<DailyTopRiskyCustomerReportResponse> data = reportRepository.findDailyTopRiskyCustomers(startDate, endDate, limit);
        return PageResponse.of(data, DEFAULT_PAGE, limit, data.size());
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ApiException(400, ErrorCode.INVALID_DATE_RANGE, "Start date must not be after end date");
        }
    }

    private static void validatePage(int page, int size) {
        if (page < 0) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Page number must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(400, ErrorCode.INVALID_PAGE_SIZE, "Page size must be between 1 and 100");
        }
    }

    private static int positiveOrDefault(Integer value, int defaultValue, String message) {
        if (value == null) {
            return defaultValue;
        }
        if (value < 1) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, message);
        }
        return value;
    }

    private static BigDecimal positiveOrDefault(BigDecimal value, BigDecimal defaultValue, String message) {
        if (value == null) {
            return defaultValue;
        }
        if (value.signum() <= 0) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, message);
        }
        return value;
    }
}
