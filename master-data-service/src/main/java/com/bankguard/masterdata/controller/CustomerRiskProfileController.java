package com.bankguard.masterdata.controller;

import com.bankguard.masterdata.dto.RiskProfileResponse;
import com.bankguard.masterdata.dto.UpdateRiskProfileRequest;
import com.bankguard.masterdata.service.CustomerRiskProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/risk-profile")
public class CustomerRiskProfileController {
    private final CustomerRiskProfileService service;

    public CustomerRiskProfileController(CustomerRiskProfileService service) {
        this.service = service;
    }

    @GetMapping
    public RiskProfileResponse get(@PathVariable Long customerId) {
        return service.get(customerId);
    }

    @PutMapping
    public RiskProfileResponse update(
            @PathVariable Long customerId,
            @Valid @RequestBody UpdateRiskProfileRequest request
    ) {
        return service.update(customerId, request);
    }
}
