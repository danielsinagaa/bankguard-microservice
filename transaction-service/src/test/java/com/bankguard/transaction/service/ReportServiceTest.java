package com.bankguard.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.transaction.repository.ReportRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReportServiceTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:10:00Z"), ZoneOffset.UTC);

    private final ReportRepository reportRepository = org.mockito.Mockito.mock(ReportRepository.class);
    private final ReportService reportService = new ReportService(reportRepository, FIXED_CLOCK);

    @Test
    void shouldValidateMissingReportDates() {
        assertThatThrownBy(() -> reportService.highRiskTransactions(50, null, LocalDate.of(2026, 6, 4), 0, 20))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void shouldValidateInvalidDateRange() {
        assertThatThrownBy(() -> reportService.dailyFraudTrend(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 4)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE));
    }

    @Test
    void shouldValidatePageSize() {
        assertThatThrownBy(() -> reportService.topRiskCustomers(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 4),
                null,
                0,
                101
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_PAGE_SIZE));
    }

    @Test
    void shouldUseDefaultVelocityThresholdsAndFixedClockWindow() {
        when(reportRepository.findTransactionVelocity(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()
        )).thenReturn(List.of());
        when(reportRepository.countTransactionVelocity(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(0L);

        reportService.transactionVelocity(null, null, null, 0, 20);

        ArgumentCaptor<Instant> windowCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(reportRepository).findTransactionVelocity(
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("50000000")),
                windowCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(20)
        );
        assertThat(windowCaptor.getValue()).isEqualTo(Instant.parse("2026-06-04T10:00:00Z"));
    }
}
