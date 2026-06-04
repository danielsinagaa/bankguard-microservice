package com.bankguard.transaction.service;

import com.bankguard.transaction.repository.TransactionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class TransactionReferenceGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final Clock clock;
    private final TransactionSequenceProvider sequenceProvider;
    private final TransactionRepository transactionRepository;

    public TransactionReferenceGenerator(Clock clock, TransactionSequenceProvider sequenceProvider, TransactionRepository transactionRepository) {
        this.clock = clock;
        this.sequenceProvider = sequenceProvider;
        this.transactionRepository = transactionRepository;
    }

    public String generate() {
        LocalDate businessDate = LocalDate.now(clock);
        long sequence = sequenceProvider.nextSequence(businessDate);
        String datePart = businessDate.format(DATE_FORMATTER);
        String candidate;
        do {
            candidate = "TRX-" + datePart + "-" + "%06d".formatted(sequence++);
        } while (transactionRepository.existsByTransactionRef(candidate));
        return candidate;
    }
}
