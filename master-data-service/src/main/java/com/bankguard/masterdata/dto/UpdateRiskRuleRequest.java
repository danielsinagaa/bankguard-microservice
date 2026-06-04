package com.bankguard.masterdata.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateRiskRuleRequest(
        @NotNull
        @Min(0)
        Integer score,

        BigDecimal thresholdValue,

        @NotNull
        Boolean active,

        @Size(max = 500)
        String description
) {
}
