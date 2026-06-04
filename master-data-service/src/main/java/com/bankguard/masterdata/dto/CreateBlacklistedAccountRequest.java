package com.bankguard.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBlacklistedAccountRequest(
        @NotBlank
        String accountNumber,

        @NotBlank
        @Size(max = 500)
        String reason
) {
}
