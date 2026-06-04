package com.bankguard.masterdata.controller;

import com.bankguard.common.api.PageResponse;
import com.bankguard.masterdata.dto.RegisterTrustedDeviceRequest;
import com.bankguard.masterdata.dto.TrustedDeviceResponse;
import com.bankguard.masterdata.dto.UpdateTrustedDeviceRequest;
import com.bankguard.masterdata.service.TrustedDeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/trusted-devices")
public class TrustedDeviceController {
    private final TrustedDeviceService service;

    public TrustedDeviceController(TrustedDeviceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TrustedDeviceResponse> register(
            @PathVariable Long customerId,
            @Valid @RequestBody RegisterTrustedDeviceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(customerId, request));
    }

    @GetMapping
    public PageResponse<TrustedDeviceResponse> list(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(customerId, page, size);
    }

    @PutMapping("/{deviceId}")
    public TrustedDeviceResponse update(
            @PathVariable Long customerId,
            @PathVariable String deviceId,
            @Valid @RequestBody UpdateTrustedDeviceRequest request
    ) {
        return service.update(customerId, deviceId, request);
    }
}
