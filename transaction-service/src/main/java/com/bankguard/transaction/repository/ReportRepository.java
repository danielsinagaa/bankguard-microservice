package com.bankguard.transaction.repository;

import com.bankguard.common.util.AccountMaskingUtil;
import com.bankguard.transaction.dto.response.DailyFraudTrendReportResponse;
import com.bankguard.transaction.dto.response.DailyTopRiskyCustomerReportResponse;
import com.bankguard.transaction.dto.response.HighRiskTransactionReportResponse;
import com.bankguard.transaction.dto.response.RiskScoreDistributionReportResponse;
import com.bankguard.transaction.dto.response.SuspiciousDestinationReportResponse;
import com.bankguard.transaction.dto.response.TopRiskCustomerReportResponse;
import com.bankguard.transaction.dto.response.TransactionVelocityReportResponse;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ReportRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HighRiskTransactionReportResponse> findHighRiskTransactions(
            int minimumRiskScore,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        String sql = """
                SELECT t.transaction_ref,
                       c.cif_number AS customer_cif,
                       c.full_name AS customer_name,
                       a.account_number AS source_account_number,
                       t.destination_account_number,
                       t.amount,
                       t.risk_score,
                       COALESCE(t.risk_decision, t.status) AS decision,
                       t.status,
                       t.created_at
                FROM transactions t
                JOIN accounts a ON a.id = t.source_account_id
                JOIN customers c ON c.id = a.customer_id
                WHERE t.risk_score >= :minimumRiskScore
                  AND t.created_at >= :startDateTime
                  AND t.created_at < :endDateTime
                ORDER BY t.risk_score DESC, t.amount DESC, t.created_at DESC, t.transaction_ref ASC
                LIMIT :limit OFFSET :offset
                """;
        return jdbcTemplate.query(sql, dateRangeParams(startDate, endDate)
                .addValue("minimumRiskScore", minimumRiskScore)
                .addValue("limit", size)
                .addValue("offset", offset(page, size)), highRiskMapper());
    }

    public long countHighRiskTransactions(int minimumRiskScore, LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT COUNT(*)
                FROM transactions t
                WHERE t.risk_score >= :minimumRiskScore
                  AND t.created_at >= :startDateTime
                  AND t.created_at < :endDateTime
                """;
        return queryCount(sql, dateRangeParams(startDate, endDate).addValue("minimumRiskScore", minimumRiskScore));
    }

    public List<TransactionVelocityReportResponse> findTransactionVelocity(
            int minimumCount,
            BigDecimal minimumAmount,
            Instant windowStart,
            int page,
            int size
    ) {
        String sql = """
                SELECT a.account_number AS source_account_number,
                       COUNT(t.id) AS transaction_count,
                       COALESCE(SUM(t.amount), 0) AS total_amount,
                       COALESCE(MAX(t.risk_score), 0) AS max_risk_score
                FROM transactions t
                JOIN accounts a ON a.id = t.source_account_id
                WHERE t.created_at >= :windowStart
                GROUP BY a.account_number
                HAVING COUNT(t.id) >= :minimumCount
                    OR COALESCE(SUM(t.amount), 0) >= :minimumAmount
                ORDER BY transaction_count DESC, total_amount DESC, source_account_number ASC
                LIMIT :limit OFFSET :offset
                """;
        return jdbcTemplate.query(sql, velocityParams(minimumCount, minimumAmount, windowStart)
                .addValue("limit", size)
                .addValue("offset", offset(page, size)), velocityMapper());
    }

    public long countTransactionVelocity(int minimumCount, BigDecimal minimumAmount, Instant windowStart) {
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT a.account_number
                    FROM transactions t
                    JOIN accounts a ON a.id = t.source_account_id
                    WHERE t.created_at >= :windowStart
                    GROUP BY a.account_number
                    HAVING COUNT(t.id) >= :minimumCount
                        OR COALESCE(SUM(t.amount), 0) >= :minimumAmount
                ) velocity
                """;
        return queryCount(sql, velocityParams(minimumCount, minimumAmount, windowStart));
    }

    public List<TopRiskCustomerReportResponse> findTopRiskCustomers(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minimumAverageRiskScore,
            int page,
            int size
    ) {
        String sql = """
                SELECT c.cif_number AS customer_cif,
                       c.full_name AS customer_name,
                       COUNT(t.id) AS total_transactions,
                       AVG(t.risk_score) AS average_risk_score,
                       MAX(t.risk_score) AS highest_risk_score,
                       COALESCE(SUM(t.amount), 0) AS total_amount
                FROM transactions t
                JOIN accounts a ON a.id = t.source_account_id
                JOIN customers c ON c.id = a.customer_id
                WHERE t.created_at >= :startDateTime
                  AND t.created_at < :endDateTime
                GROUP BY c.cif_number, c.full_name
                HAVING AVG(t.risk_score) >= :minimumAverageRiskScore
                ORDER BY average_risk_score DESC, highest_risk_score DESC, total_amount DESC, customer_cif ASC
                LIMIT :limit OFFSET :offset
                """;
        return jdbcTemplate.query(sql, dateRangeParams(startDate, endDate)
                .addValue("minimumAverageRiskScore", minimumAverageRiskScore)
                .addValue("limit", size)
                .addValue("offset", offset(page, size)), topRiskCustomerMapper());
    }

    public long countTopRiskCustomers(LocalDate startDate, LocalDate endDate, BigDecimal minimumAverageRiskScore) {
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT c.cif_number
                    FROM transactions t
                    JOIN accounts a ON a.id = t.source_account_id
                    JOIN customers c ON c.id = a.customer_id
                    WHERE t.created_at >= :startDateTime
                      AND t.created_at < :endDateTime
                    GROUP BY c.cif_number
                    HAVING AVG(t.risk_score) >= :minimumAverageRiskScore
                ) risky_customers
                """;
        return queryCount(sql, dateRangeParams(startDate, endDate).addValue("minimumAverageRiskScore", minimumAverageRiskScore));
    }

    public List<SuspiciousDestinationReportResponse> findSuspiciousDestinationAccounts(
            LocalDate startDate,
            LocalDate endDate,
            int minimumUniqueSenders,
            BigDecimal minimumTotalAmount,
            int page,
            int size
    ) {
        String sql = """
                SELECT t.destination_account_number,
                       COUNT(t.id) AS total_received,
                       COUNT(DISTINCT t.source_account_id) AS unique_senders,
                       COALESCE(SUM(t.amount), 0) AS total_amount,
                       AVG(t.risk_score) AS average_risk_score
                FROM transactions t
                WHERE t.created_at >= :startDateTime
                  AND t.created_at < :endDateTime
                GROUP BY t.destination_account_number
                HAVING COUNT(DISTINCT t.source_account_id) >= :minimumUniqueSenders
                    OR COALESCE(SUM(t.amount), 0) >= :minimumTotalAmount
                ORDER BY unique_senders DESC, total_amount DESC, average_risk_score DESC, destination_account_number ASC
                LIMIT :limit OFFSET :offset
                """;
        return jdbcTemplate.query(sql, suspiciousDestinationParams(startDate, endDate, minimumUniqueSenders, minimumTotalAmount)
                .addValue("limit", size)
                .addValue("offset", offset(page, size)), suspiciousDestinationMapper());
    }

    public long countSuspiciousDestinationAccounts(
            LocalDate startDate,
            LocalDate endDate,
            int minimumUniqueSenders,
            BigDecimal minimumTotalAmount
    ) {
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT t.destination_account_number
                    FROM transactions t
                    WHERE t.created_at >= :startDateTime
                      AND t.created_at < :endDateTime
                    GROUP BY t.destination_account_number
                    HAVING COUNT(DISTINCT t.source_account_id) >= :minimumUniqueSenders
                        OR COALESCE(SUM(t.amount), 0) >= :minimumTotalAmount
                ) suspicious_destinations
                """;
        return queryCount(sql, suspiciousDestinationParams(startDate, endDate, minimumUniqueSenders, minimumTotalAmount));
    }

    public List<DailyFraudTrendReportResponse> findDailyFraudTrend(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT CAST(t.created_at AS DATE) AS transaction_date,
                       COUNT(t.id) AS total_transactions,
                       SUM(CASE WHEN COALESCE(t.risk_decision, t.status) = 'APPROVED' THEN 1 ELSE 0 END) AS approved_count,
                       SUM(CASE WHEN COALESCE(t.risk_decision, t.status) = 'REVIEW' THEN 1 ELSE 0 END) AS review_count,
                       SUM(CASE WHEN COALESCE(t.risk_decision, t.status) = 'BLOCKED' THEN 1 ELSE 0 END) AS blocked_count,
                       AVG(t.risk_score) AS average_risk_score,
                       COALESCE(SUM(t.amount), 0) AS total_amount
                FROM transactions t
                WHERE t.created_at >= :startDateTime
                  AND t.created_at < :endDateTime
                GROUP BY CAST(t.created_at AS DATE)
                ORDER BY transaction_date ASC
                """;
        return jdbcTemplate.query(sql, dateRangeParams(startDate, endDate), dailyFraudTrendMapper());
    }

    public List<RiskScoreDistributionReportResponse> findRiskScoreDistribution(LocalDate startDate, LocalDate endDate) {
        String sql = """
                WITH buckets(risk_bucket, minimum_score, maximum_score, bucket_order) AS (
                    VALUES ('LOW', 0, 49, 1),
                           ('MEDIUM', 50, 79, 2),
                           ('HIGH', 80, 100, 3)
                ),
                bucketed AS (
                    SELECT CASE
                               WHEN t.risk_score BETWEEN 0 AND 49 THEN 'LOW'
                               WHEN t.risk_score BETWEEN 50 AND 79 THEN 'MEDIUM'
                               ELSE 'HIGH'
                           END AS risk_bucket,
                           t.risk_score
                    FROM transactions t
                    WHERE t.created_at >= :startDateTime
                      AND t.created_at < :endDateTime
                )
                SELECT b.risk_bucket,
                       COUNT(bt.risk_score) AS total_transactions,
                       b.minimum_score,
                       b.maximum_score,
                       COALESCE(AVG(bt.risk_score), 0) AS average_score
                FROM buckets b
                LEFT JOIN bucketed bt ON bt.risk_bucket = b.risk_bucket
                GROUP BY b.risk_bucket, b.minimum_score, b.maximum_score, b.bucket_order
                ORDER BY b.bucket_order ASC
                """;
        return jdbcTemplate.query(sql, dateRangeParams(startDate, endDate), riskScoreDistributionMapper());
    }

    public List<DailyTopRiskyCustomerReportResponse> findDailyTopRiskyCustomers(
            LocalDate startDate,
            LocalDate endDate,
            int topN
    ) {
        String sql = """
                WITH customer_daily AS (
                    SELECT CAST(t.created_at AS DATE) AS transaction_date,
                           c.cif_number AS customer_cif,
                           c.full_name AS customer_name,
                           COUNT(t.id) AS total_transactions,
                           COALESCE(SUM(t.amount), 0) AS total_amount,
                           AVG(t.risk_score) AS average_risk_score,
                           MAX(t.risk_score) AS highest_risk_score
                    FROM transactions t
                    JOIN accounts a ON a.id = t.source_account_id
                    JOIN customers c ON c.id = a.customer_id
                    WHERE t.created_at >= :startDateTime
                      AND t.created_at < :endDateTime
                    GROUP BY CAST(t.created_at AS DATE), c.cif_number, c.full_name
                ),
                ranked AS (
                    SELECT customer_daily.*,
                           ROW_NUMBER() OVER (
                               PARTITION BY transaction_date
                               ORDER BY average_risk_score DESC,
                                        highest_risk_score DESC,
                                        total_amount DESC,
                                        customer_cif ASC
                           ) AS risk_rank
                    FROM customer_daily
                )
                SELECT transaction_date,
                       risk_rank,
                       customer_cif,
                       customer_name,
                       total_transactions,
                       total_amount,
                       average_risk_score,
                       highest_risk_score
                FROM ranked
                WHERE risk_rank <= :topN
                ORDER BY transaction_date ASC, risk_rank ASC
                """;
        return jdbcTemplate.query(sql, dateRangeParams(startDate, endDate).addValue("topN", topN), dailyTopRiskyCustomerMapper());
    }

    private long queryCount(String sql, MapSqlParameterSource params) {
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0 : count;
    }

    private static MapSqlParameterSource dateRangeParams(LocalDate startDate, LocalDate endDate) {
        return new MapSqlParameterSource()
                .addValue("startDateTime", Timestamp.valueOf(startDate.atStartOfDay()))
                .addValue("endDateTime", Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
    }

    private static MapSqlParameterSource velocityParams(int minimumCount, BigDecimal minimumAmount, Instant windowStart) {
        return new MapSqlParameterSource()
                .addValue("minimumCount", minimumCount)
                .addValue("minimumAmount", minimumAmount)
                .addValue("windowStart", Timestamp.valueOf(LocalDateTime.ofInstant(windowStart, ZoneOffset.UTC)));
    }

    private static MapSqlParameterSource suspiciousDestinationParams(
            LocalDate startDate,
            LocalDate endDate,
            int minimumUniqueSenders,
            BigDecimal minimumTotalAmount
    ) {
        return dateRangeParams(startDate, endDate)
                .addValue("minimumUniqueSenders", minimumUniqueSenders)
                .addValue("minimumTotalAmount", minimumTotalAmount);
    }

    private static long offset(int page, int size) {
        return (long) page * size;
    }

    private static RowMapper<HighRiskTransactionReportResponse> highRiskMapper() {
        return (rs, rowNum) -> new HighRiskTransactionReportResponse(
                rs.getString("transaction_ref"),
                rs.getString("customer_cif"),
                rs.getString("customer_name"),
                AccountMaskingUtil.mask(rs.getString("source_account_number")),
                AccountMaskingUtil.mask(rs.getString("destination_account_number")),
                rs.getBigDecimal("amount"),
                rs.getInt("risk_score"),
                rs.getString("decision"),
                rs.getString("status"),
                toInstant(rs, "created_at")
        );
    }

    private static RowMapper<TransactionVelocityReportResponse> velocityMapper() {
        return (rs, rowNum) -> new TransactionVelocityReportResponse(
                AccountMaskingUtil.mask(rs.getString("source_account_number")),
                rs.getLong("transaction_count"),
                rs.getBigDecimal("total_amount"),
                rs.getInt("max_risk_score")
        );
    }

    private static RowMapper<TopRiskCustomerReportResponse> topRiskCustomerMapper() {
        return (rs, rowNum) -> new TopRiskCustomerReportResponse(
                rs.getString("customer_cif"),
                rs.getString("customer_name"),
                rs.getLong("total_transactions"),
                rs.getBigDecimal("average_risk_score"),
                rs.getInt("highest_risk_score"),
                rs.getBigDecimal("total_amount")
        );
    }

    private static RowMapper<SuspiciousDestinationReportResponse> suspiciousDestinationMapper() {
        return (rs, rowNum) -> new SuspiciousDestinationReportResponse(
                AccountMaskingUtil.mask(rs.getString("destination_account_number")),
                rs.getLong("total_received"),
                rs.getLong("unique_senders"),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("average_risk_score")
        );
    }

    private static RowMapper<DailyFraudTrendReportResponse> dailyFraudTrendMapper() {
        return (rs, rowNum) -> new DailyFraudTrendReportResponse(
                toLocalDate(rs, "transaction_date"),
                rs.getLong("total_transactions"),
                rs.getLong("approved_count"),
                rs.getLong("review_count"),
                rs.getLong("blocked_count"),
                rs.getBigDecimal("average_risk_score"),
                rs.getBigDecimal("total_amount")
        );
    }

    private static RowMapper<RiskScoreDistributionReportResponse> riskScoreDistributionMapper() {
        return (rs, rowNum) -> new RiskScoreDistributionReportResponse(
                rs.getString("risk_bucket"),
                rs.getLong("total_transactions"),
                rs.getInt("minimum_score"),
                rs.getInt("maximum_score"),
                rs.getBigDecimal("average_score")
        );
    }

    private static RowMapper<DailyTopRiskyCustomerReportResponse> dailyTopRiskyCustomerMapper() {
        return (rs, rowNum) -> new DailyTopRiskyCustomerReportResponse(
                toLocalDate(rs, "transaction_date"),
                rs.getInt("risk_rank"),
                rs.getString("customer_cif"),
                rs.getString("customer_name"),
                rs.getLong("total_transactions"),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("average_risk_score"),
                rs.getInt("highest_risk_score")
        );
    }

    private static Instant toInstant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        if (timestamp != null) {
            return timestamp.toInstant();
        }

        LocalDateTime localDateTime = rs.getObject(column, LocalDateTime.class);
        return localDateTime == null ? null : Timestamp.valueOf(localDateTime).toInstant();
    }

    private static LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        Date date = rs.getDate(column);
        if (date != null) {
            return date.toLocalDate();
        }
        return rs.getObject(column, LocalDate.class);
    }
}
