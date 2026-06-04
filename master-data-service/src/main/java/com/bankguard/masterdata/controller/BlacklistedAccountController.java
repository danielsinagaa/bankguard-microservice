package com.bankguard.masterdata.controller;

import com.bankguard.common.api.PageResponse;
import com.bankguard.masterdata.dto.BlacklistedAccountResponse;
import com.bankguard.masterdata.dto.CreateBlacklistedAccountRequest;
import com.bankguard.masterdata.dto.UpdateBlacklistedAccountRequest;
import com.bankguard.masterdata.service.BlacklistedAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blacklisted-accounts")
public class BlacklistedAccountController {
    private final BlacklistedAccountService service;

    public BlacklistedAccountController(BlacklistedAccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BlacklistedAccountResponse> create(@Valid @RequestBody CreateBlacklistedAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public PageResponse<BlacklistedAccountResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(active, accountNumber, page, size);
    }

    @GetMapping("/{id}")
    public BlacklistedAccountResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public BlacklistedAccountResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBlacklistedAccountRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
