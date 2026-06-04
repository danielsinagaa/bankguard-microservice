package com.bankguard.auditsearch.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankguard.auditsearch.dto.AuditSearchCriteria;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AuditSearchQueryBuilderTest {

    @Test
    void shouldBuildKeywordAndFilterQuery() {
        AuditSearchQueryBuilder builder = new AuditSearchQueryBuilder();

        String query = builder.build(new AuditSearchCriteria(
                "Jakarta",
                "TRX-20260604-000001",
                "CIF001",
                "REVIEW",
                50,
                "HIGH_AMOUNT",
                "MOBILE_BANKING",
                "Jakarta",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 4),
                0,
                20,
                "createdAt",
                "DESC"
        ));

        assertThat(query).contains("\"multi_match\"");
        assertThat(query).contains("\"transactionRef\":\"TRX-20260604-000001\"");
        assertThat(query).contains("\"customerCif\":\"CIF001\"");
        assertThat(query).contains("\"decision\":\"REVIEW\"");
        assertThat(query).contains("\"riskScore\":{\"gte\":50}");
        assertThat(query).contains("\"createdAt\"");
    }
}
