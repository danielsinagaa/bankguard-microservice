package com.bankguard.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRiskProfileRequest(
        @NotBlank
        String riskLevel,

        @Size(max = 500)
        String riskReason
) {
}
