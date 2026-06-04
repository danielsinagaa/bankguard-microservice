package com.bankguard.transaction.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ReportRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("bankguard")
            .withUsername("bankguard")
            .withPassword("bankguard");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM transaction_risk_factors");
        jdbcTemplate.update("DELETE FROM transactions");
        insertTransaction(
                "TRX-REPORT-001",
                "report-key-001",
                "1234567890",
                "9876543210",
                "25000000",
                "REVIEW",
                72,
                "2026-06-04 10:00:00"
        );
        insertTransaction(
                "TRX-REPORT-002",
                "report-key-002",
                "1234567890",
                "8876543210",
                "60000000",
                "BLOCKED",
                90,
                "2026-06-04 10:05:00"
        );
        insertTransaction(
                "TRX-REPORT-003",
                "report-key-003",
                "2234567890",
                "9876543210",
                "10000000",
                "APPROVED",
                30,
                "2026-06-05 09:00:00"
        );
    }

    @Test
    void shouldRunNativeReportQueriesWithDeterministicMappings() {
        LocalDate startDate = LocalDate.of(2026, 6, 4);
        LocalDate endDate = LocalDate.of(2026, 6, 5);

        var highRisk = reportRepository.findHighRiskTransactions(50, startDate, endDate, 0, 20);
        assertThat(highRisk).extracting("transactionRef").containsExactly("TRX-REPORT-002", "TRX-REPORT-001");
        assertThat(highRisk.get(0).sourceAccountNumber()).isEqualTo("123****890");
        assertThat(reportRepository.countHighRiskTransactions(50, startDate, endDate)).isEqualTo(2);

        var topCustomers = reportRepository.findTopRiskCustomers(startDate, endDate, new BigDecimal("50"), 0, 20);
        assertThat(topCustomers).extracting("customerCif").containsExactly("CIF001");
        assertThat(topCustomers.get(0).totalTransactions()).isEqualTo(2);

        var suspiciousDestinations = reportRepository.findSuspiciousDestinationAccounts(
                startDate,
                endDate,
                2,
                new BigDecimal("30000000"),
                0,
                20
        );
        assertThat(suspiciousDestinations).extracting("destinationAccountNumber")
                .containsExactly("987****210", "887****210");

        var trend = reportRepository.findDailyFraudTrend(startDate, endDate);
        assertThat(trend).hasSize(2);
        assertThat(trend.get(0).transactionDate()).isEqualTo(LocalDate.of(2026, 6, 4));
        assertThat(trend.get(0).reviewCount()).isEqualTo(1);
        assertThat(trend.get(0).blockedCount()).isEqualTo(1);

        var distribution = reportRepository.findRiskScoreDistribution(startDate, endDate);
        assertThat(distribution).extracting("riskBucket").containsExactly("LOW", "MEDIUM", "HIGH");
        assertThat(distribution).extracting("totalTransactions").containsExactly(1L, 1L, 1L);

        var dailyTop = reportRepository.findDailyTopRiskyCustomers(startDate, endDate, 1);
        assertThat(dailyTop).extracting("customerCif").containsExactly("CIF001", "CIF002");
    }

    @Test
    void shouldRunVelocityReportWithinWindow() {
        var velocity = reportRepository.findTransactionVelocity(
                2,
                new BigDecimal("100000000"),
                Instant.parse("2026-06-04T09:59:00Z"),
                0,
                20
        );

        assertThat(velocity).singleElement()
                .satisfies(row -> {
                    assertThat(row.sourceAccountNumber()).isEqualTo("123****890");
                    assertThat(row.transactionCount()).isEqualTo(2);
                    assertThat(row.maxRiskScore()).isEqualTo(90);
                });
        assertThat(reportRepository.countTransactionVelocity(
                2,
                new BigDecimal("100000000"),
                Instant.parse("2026-06-04T09:59:00Z")
        )).isEqualTo(1);
    }

    private void insertTransaction(
            String transactionRef,
            String idempotencyKey,
            String sourceAccountNumber,
            String destinationAccountNumber,
            String amount,
            String decision,
            int riskScore,
            String createdAt
    ) {
        Long sourceAccountId = jdbcTemplate.queryForObject(
                "SELECT id FROM accounts WHERE account_number = ?",
                Long.class,
                sourceAccountNumber
        );
        jdbcTemplate.update("""
                INSERT INTO transactions (
                    transaction_ref,
                    idempotency_key,
                    source_account_id,
                    destination_account_number,
                    amount,
                    currency,
                    channel,
                    status,
                    risk_score,
                    risk_decision,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, 'IDR', 'MOBILE_BANKING', ?, ?, ?, ?::timestamp, ?::timestamp)
                """,
                transactionRef,
                idempotencyKey,
                sourceAccountId,
                destinationAccountNumber,
                new BigDecimal(amount),
                decision,
                riskScore,
                decision,
                createdAt,
                createdAt
        );
    }
}
