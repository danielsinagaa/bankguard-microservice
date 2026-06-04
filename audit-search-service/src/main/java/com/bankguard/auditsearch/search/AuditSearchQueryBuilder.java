package com.bankguard.auditsearch.search;

import com.bankguard.auditsearch.dto.AuditSearchCriteria;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AuditSearchQueryBuilder {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public String build(AuditSearchCriteria criteria) {
        List<Map<String, Object>> must = new ArrayList<>();
        List<Map<String, Object>> filter = new ArrayList<>();

        if (hasText(criteria.keyword())) {
            must.add(mapOf("multi_match", mapOf(
                    "query", criteria.keyword(),
                    "fields", List.of("transactionRef", "customerName", "location", "riskFactors")
            )));
        }
        if (!hasText(criteria.keyword())) {
            must.add(mapOf("match_all", Map.of()));
        }

        addTerm(filter, "transactionRef", criteria.transactionRef());
        addTerm(filter, "customerCif", criteria.customerCif());
        addTerm(filter, "decision", criteria.decision());
        addTerm(filter, "riskFactors", criteria.riskFactor());
        addTerm(filter, "channel", criteria.channel());
        if (hasText(criteria.location())) {
            must.add(mapOf("match", mapOf("location", criteria.location())));
        }
        if (criteria.minimumRiskScore() != null) {
            filter.add(mapOf("range", mapOf("riskScore", mapOf("gte", criteria.minimumRiskScore()))));
        }
        addDateRange(filter, criteria.startDate(), criteria.endDate());

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("must", must);
        if (!filter.isEmpty()) {
            bool.put("filter", filter);
        }
        try {
            return objectMapper.writeValueAsString(mapOf("bool", bool));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to build audit search query", ex);
        }
    }

    private void addTerm(List<Map<String, Object>> filter, String field, String value) {
        if (hasText(value)) {
            filter.add(mapOf("term", mapOf(field, value)));
        }
    }

    private void addDateRange(List<Map<String, Object>> filter, LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return;
        }
        Map<String, Object> range = new LinkedHashMap<>();
        if (startDate != null) {
            range.put("gte", startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toString());
        }
        if (endDate != null) {
            range.put("lte", endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1).toString());
        }
        filter.add(mapOf("range", mapOf("createdAt", range)));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(values[i].toString(), values[i + 1]);
        }
        return map;
    }
}
