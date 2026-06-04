package com.bankguard.transaction.service;

import java.time.LocalDate;

public interface TransactionSequenceProvider {
    long nextSequence(LocalDate businessDate);
}
