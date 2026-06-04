package com.bankguard.auditsearch.controller;

import com.bankguard.auditsearch.dto.AuditSearchCriteria;
import com.bankguard.auditsearch.dto.AuditSearchResponse;
import com.bankguard.auditsearch.service.AuditSearchService;
import com.bankguard.common.api.PageResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audits")
public class AuditSearchController {
    private final AuditSearchService auditSearchService;

    public AuditSearchController(AuditSearchService auditSearchService) {
        this.auditSearchService = auditSearchService;
    }

    @GetMapping("/search")
    public PageResponse<AuditSearchResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String transactionRef,
            @RequestParam(required = false) String customerCif,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) Integer minimumRiskScore,
            @RequestParam(required = false) String riskFactor,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        return auditSearchService.search(new AuditSearchCriteria(
                keyword,
                transactionRef,
                customerCif,
                decision,
                minimumRiskScore,
                riskFactor,
                channel,
                location,
                startDate,
                endDate,
                page,
                size,
                sortBy,
                sortDirection
        ));
    }
}
