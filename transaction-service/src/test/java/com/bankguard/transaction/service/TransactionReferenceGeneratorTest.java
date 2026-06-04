package com.bankguard.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bankguard.transaction.repository.TransactionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class TransactionReferenceGeneratorTest {

    @Test
    void shouldGenerateReferenceUsingBusinessDateAndSequence() {
        TransactionSequenceProvider sequenceProvider = mock(TransactionSequenceProvider.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        when(sequenceProvider.nextSequence(LocalDate.of(2026, 6, 4))).thenReturn(1L);
        when(transactionRepository.existsByTransactionRef("TRX-20260604-000001")).thenReturn(false);
        TransactionReferenceGenerator generator = new TransactionReferenceGenerator(
                Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC),
                sequenceProvider,
                transactionRepository
        );

        assertThat(generator.generate()).isEqualTo("TRX-20260604-000001");
    }

    @Test
    void shouldSkipExistingReference() {
        TransactionSequenceProvider sequenceProvider = mock(TransactionSequenceProvider.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        when(sequenceProvider.nextSequence(LocalDate.of(2026, 6, 4))).thenReturn(1L);
        when(transactionRepository.existsByTransactionRef("TRX-20260604-000001")).thenReturn(true);
        when(transactionRepository.existsByTransactionRef("TRX-20260604-000002")).thenReturn(false);
        TransactionReferenceGenerator generator = new TransactionReferenceGenerator(
                Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC),
                sequenceProvider,
                transactionRepository
        );

        assertThat(generator.generate()).isEqualTo("TRX-20260604-000002");
    }
}
