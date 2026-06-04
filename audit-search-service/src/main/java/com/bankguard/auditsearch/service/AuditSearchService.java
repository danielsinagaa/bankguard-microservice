package com.bankguard.auditsearch.service;

import com.bankguard.auditsearch.document.AuditDocument;
import com.bankguard.auditsearch.dto.AuditSearchCriteria;
import com.bankguard.auditsearch.dto.AuditSearchResponse;
import com.bankguard.auditsearch.search.AuditSearchQueryBuilder;
import com.bankguard.common.api.PageResponse;
import com.bankguard.common.constant.Channel;
import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.constant.RiskDecision;
import com.bankguard.common.exception.ApiException;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

@Service
public class AuditSearchService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "scoredAt", "riskScore", "amount");

    private final ElasticsearchOperations elasticsearchOperations;
    private final AuditSearchQueryBuilder queryBuilder;

    public AuditSearchService(ElasticsearchOperations elasticsearchOperations, AuditSearchQueryBuilder queryBuilder) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.queryBuilder = queryBuilder;
    }

    public PageResponse<AuditSearchResponse> search(AuditSearchCriteria criteria) {
        validate(criteria);
        Sort.Direction direction = "ASC".equalsIgnoreCase(criteria.sortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortBy = hasText(criteria.sortBy()) ? criteria.sortBy() : "createdAt";
        PageRequest pageRequest = PageRequest.of(
                criteria.page(),
                criteria.size(),
                Sort.by(direction, sortBy)
        );
        StringQuery query = new StringQuery(queryBuilder.build(criteria));
        query.setPageable(pageRequest);

        try {
            SearchHits<AuditDocument> hits = elasticsearchOperations.search(query, AuditDocument.class);
            List<AuditSearchResponse> data = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(this::toResponse)
                    .toList();
            return PageResponse.of(data, criteria.page(), criteria.size(), hits.getTotalHits());
        } catch (RuntimeException ex) {
            throw new ApiException(503, ErrorCode.DEPENDENCY_UNAVAILABLE, "Elasticsearch is unavailable");
        }
    }

    private void validate(AuditSearchCriteria criteria) {
        if (criteria.page() < 0) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Page number must not be negative");
        }
        if (criteria.size() < 1 || criteria.size() > MAX_PAGE_SIZE) {
            throw new ApiException(400, ErrorCode.INVALID_PAGE_SIZE, "Page size must be between 1 and 100");
        }
        if (criteria.startDate() != null && criteria.endDate() != null && criteria.startDate().isAfter(criteria.endDate())) {
            throw new ApiException(400, ErrorCode.INVALID_DATE_RANGE, "Start date must not be after end date");
        }
        if (hasText(criteria.decision())) {
            try {
                RiskDecision.valueOf(criteria.decision());
            } catch (RuntimeException ex) {
                throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Decision is invalid");
            }
        }
        if (hasText(criteria.channel())) {
            try {
                Channel.valueOf(criteria.channel());
            } catch (RuntimeException ex) {
                throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Channel is invalid");
            }
        }
        if (hasText(criteria.sortDirection())
                && !"ASC".equalsIgnoreCase(criteria.sortDirection())
                && !"DESC".equalsIgnoreCase(criteria.sortDirection())) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Sort direction is invalid");
        }
        String sortBy = hasText(criteria.sortBy()) ? criteria.sortBy() : "createdAt";
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Sort field is invalid");
        }
    }

    private AuditSearchResponse toResponse(AuditDocument document) {
        return new AuditSearchResponse(
                document.getTransactionRef(),
                document.getCustomerCif(),
                document.getCustomerName(),
                document.getSourceAccountMasked(),
                document.getDestinationAccountMasked(),
                document.getAmount(),
                document.getCurrency(),
                document.getChannel(),
                document.getLocation(),
                document.getRiskScore(),
                document.getDecision(),
                document.getRiskFactors(),
                document.getCreatedAt(),
                document.getScoredAt()
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
