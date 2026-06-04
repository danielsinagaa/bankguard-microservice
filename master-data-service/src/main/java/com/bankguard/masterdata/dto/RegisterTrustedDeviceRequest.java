package com.bankguard.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTrustedDeviceRequest(
        @NotBlank
        @Size(max = 100)
        String deviceId,

        @Size(max = 100)
        String deviceName
) {
}
