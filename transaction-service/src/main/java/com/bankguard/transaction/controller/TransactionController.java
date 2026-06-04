package com.bankguard.transaction.controller;

import com.bankguard.common.constant.ApiHeaders;
import com.bankguard.common.web.RequestIdFilter;
import com.bankguard.transaction.dto.request.SubmitTransactionRequest;
import com.bankguard.transaction.dto.response.SubmitTransactionResponse;
import com.bankguard.transaction.dto.response.TransactionDetailResponse;
import com.bankguard.transaction.service.SubmitTransactionResult;
import com.bankguard.transaction.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<SubmitTransactionResponse> submit(
            @RequestHeader(value = ApiHeaders.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @Valid @RequestBody SubmitTransactionRequest request,
            HttpServletRequest servletRequest
    ) {
        SubmitTransactionResult result = transactionService.submit(request, idempotencyKey, RequestIdFilter.requestId(servletRequest));
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.response());
    }

    @GetMapping("/{transactionRef}")
    public ResponseEntity<TransactionDetailResponse> detail(@PathVariable String transactionRef) {
        return ResponseEntity.ok(transactionService.getDetail(transactionRef));
    }
}
