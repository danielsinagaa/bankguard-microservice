package com.bankguard.transaction.mapper;

import com.bankguard.common.util.AccountMaskingUtil;
import com.bankguard.transaction.dto.response.RiskFactorResponse;
import com.bankguard.transaction.dto.response.SubmitTransactionResponse;
import com.bankguard.transaction.dto.response.TransactionDetailResponse;
import com.bankguard.transaction.entity.TransactionEntity;
import com.bankguard.transaction.entity.TransactionRiskFactorEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    private static final String SUBMITTED_MESSAGE = "Transaction submitted for risk scoring";
    private static final String IDEMPOTENT_MESSAGE = "Existing transaction returned for the provided idempotency key";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public SubmitTransactionResponse toSubmitResponse(TransactionEntity transaction, boolean created) {
        return new SubmitTransactionResponse(
                transaction.getTransactionRef(),
                transaction.getStatus(),
                created ? SUBMITTED_MESSAGE : IDEMPOTENT_MESSAGE
        );
    }

    public TransactionDetailResponse toDetailResponse(TransactionEntity transaction) {
        return new TransactionDetailResponse(
                transaction.getTransactionRef(),
                AccountMaskingUtil.mask(transaction.getSourceAccount().getAccountNumber()),
                AccountMaskingUtil.mask(transaction.getDestinationAccountNumber()),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getChannel(),
                transaction.getDeviceId(),
                transaction.getIpAddress(),
                transaction.getLocation(),
                transaction.getStatus(),
                transaction.getRiskScore(),
                transaction.getRiskDecision(),
                transaction.getRiskFactors().stream().map(this::toRiskFactorResponse).toList(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    private RiskFactorResponse toRiskFactorResponse(TransactionRiskFactorEntity riskFactor) {
        return new RiskFactorResponse(
                riskFactor.getFactorCode(),
                riskFactor.getFactorDescription(),
                riskFactor.getScore(),
                parseMetadata(riskFactor.getMetadata())
        );
    }

    private Map<String, Object> parseMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(metadata, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
