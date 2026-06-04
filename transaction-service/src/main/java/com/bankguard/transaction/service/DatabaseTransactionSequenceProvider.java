package com.bankguard.transaction.service;

import com.bankguard.transaction.repository.TransactionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class DatabaseTransactionSequenceProvider implements TransactionSequenceProvider {
    private final TransactionRepository transactionRepository;

    public DatabaseTransactionSequenceProvider(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public long nextSequence(LocalDate businessDate) {
        Instant from = businessDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = businessDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return transactionRepository.countByCreatedAtBetween(from, to) + 1;
    }
}
