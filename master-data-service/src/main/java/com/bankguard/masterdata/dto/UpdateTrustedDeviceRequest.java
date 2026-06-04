package com.bankguard.masterdata.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTrustedDeviceRequest(
        @Size(max = 100)
        String deviceName,

        @NotNull
        Boolean trusted
) {
}
