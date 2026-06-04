package com.bankguard.auditsearch.mapper;

import com.bankguard.auditsearch.document.AuditDocument;
import com.bankguard.common.event.RiskFactorPayload;
import com.bankguard.common.event.TransactionRiskScoredPayload;
import com.bankguard.common.util.AccountMaskingUtil;
import org.springframework.stereotype.Component;

@Component
public class AuditDocumentMapper {

    public AuditDocument toDocument(TransactionRiskScoredPayload payload) {
        return new AuditDocument(
                payload.transactionRef(),
                payload.customerCif(),
                payload.customerName(),
                AccountMaskingUtil.mask(payload.sourceAccountNumber()),
                AccountMaskingUtil.mask(payload.destinationAccountNumber()),
                payload.amount(),
                payload.currency(),
                payload.channel(),
                payload.location(),
                payload.riskScore(),
                payload.decision(),
                payload.riskFactors().stream().map(RiskFactorPayload::code).sorted().toList(),
                payload.createdAt(),
                payload.scoredAt()
        );
    }
}
