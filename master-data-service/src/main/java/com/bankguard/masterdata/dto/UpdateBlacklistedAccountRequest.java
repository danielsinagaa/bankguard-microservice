package com.bankguard.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBlacklistedAccountRequest(
        @NotBlank
        @Size(max = 500)
        String reason,

        @NotNull
        Boolean active
) {
}
