package com.bankguard.masterdata.controller;

import com.bankguard.common.api.PageResponse;
import com.bankguard.masterdata.dto.RiskRuleResponse;
import com.bankguard.masterdata.dto.UpdateRiskRuleRequest;
import com.bankguard.masterdata.service.RiskRuleConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk-rules")
public class RiskRuleController {
    private final RiskRuleConfigService service;

    public RiskRuleController(RiskRuleConfigService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<RiskRuleResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(page, size);
    }

    @GetMapping("/{ruleCode}")
    public RiskRuleResponse get(@PathVariable String ruleCode) {
        return service.get(ruleCode);
    }

    @PutMapping("/{ruleCode}")
    public RiskRuleResponse update(
            @PathVariable String ruleCode,
            @Valid @RequestBody UpdateRiskRuleRequest request
    ) {
        return service.update(ruleCode, request);
    }
}
