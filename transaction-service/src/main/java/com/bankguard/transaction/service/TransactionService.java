package com.bankguard.transaction.service;

import com.bankguard.common.constant.Channel;
import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.constant.TransactionStatus;
import com.bankguard.common.exception.ApiException;
import com.bankguard.transaction.dto.request.SubmitTransactionRequest;
import com.bankguard.transaction.dto.response.TransactionDetailResponse;
import com.bankguard.transaction.entity.AccountEntity;
import com.bankguard.transaction.entity.KafkaEventLogEntity;
import com.bankguard.transaction.entity.TransactionEntity;
import com.bankguard.transaction.kafka.PublishedTransactionEvent;
import com.bankguard.transaction.kafka.TransactionCreatedEventPublisher;
import com.bankguard.transaction.mapper.TransactionMapper;
import com.bankguard.transaction.repository.KafkaEventLogRepository;
import com.bankguard.transaction.repository.TransactionRepository;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private static final String DEFAULT_CURRENCY = "IDR";

    private final TransactionRepository transactionRepository;
    private final KafkaEventLogRepository kafkaEventLogRepository;
    private final AccountValidationService accountValidationService;
    private final TransactionReferenceGenerator transactionReferenceGenerator;
    private final TransactionCreatedEventPublisher eventPublisher;
    private final TransactionMapper transactionMapper;
    private final Clock clock;

    public TransactionService(
            TransactionRepository transactionRepository,
            KafkaEventLogRepository kafkaEventLogRepository,
            AccountValidationService accountValidationService,
            TransactionReferenceGenerator transactionReferenceGenerator,
            TransactionCreatedEventPublisher eventPublisher,
            TransactionMapper transactionMapper,
            Clock clock
    ) {
        this.transactionRepository = transactionRepository;
        this.kafkaEventLogRepository = kafkaEventLogRepository;
        this.accountValidationService = accountValidationService;
        this.transactionReferenceGenerator = transactionReferenceGenerator;
        this.eventPublisher = eventPublisher;
        this.transactionMapper = transactionMapper;
        this.clock = clock;
    }

    @Transactional
    public SubmitTransactionResult submit(SubmitTransactionRequest request, String idempotencyKey, String requestId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(400, ErrorCode.IDEMPOTENCY_KEY_REQUIRED, "Idempotency-Key header is required");
        }

        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> new SubmitTransactionResult(transactionMapper.toSubmitResponse(existing, false), false))
                .orElseGet(() -> createTransaction(request, idempotencyKey, requestId));
    }

    @Transactional(readOnly = true)
    public TransactionDetailResponse getDetail(String transactionRef) {
        TransactionEntity transaction = transactionRepository.findDetailByTransactionRef(transactionRef)
                .orElseThrow(() -> new ApiException(404, ErrorCode.TRANSACTION_NOT_FOUND, "Transaction does not exist"));
        return transactionMapper.toDetailResponse(transaction);
    }

    private SubmitTransactionResult createTransaction(SubmitTransactionRequest request, String idempotencyKey, String requestId) {
        String channel = validateChannel(request.channel());
        validateCurrency(request.currency());
        validateIpAddress(request.ipAddress());
        AccountEntity sourceAccount = accountValidationService.validateSourceAccount(request.sourceAccountNumber());

        TransactionEntity transaction = transactionRepository.save(new TransactionEntity(
                transactionReferenceGenerator.generate(),
                idempotencyKey,
                sourceAccount,
                request.destinationAccountNumber(),
                request.amount(),
                DEFAULT_CURRENCY,
                channel,
                blankToNull(request.deviceId()),
                blankToNull(request.ipAddress()),
                blankToNull(request.location()),
                TransactionStatus.PENDING_RISK_CHECK.name(),
                Instant.now(clock)
        ));

        PublishedTransactionEvent publishedEvent = eventPublisher.publishCreated(transaction, requestId);
        kafkaEventLogRepository.save(new KafkaEventLogEntity(
                publishedEvent.envelope().eventId(),
                publishedEvent.envelope().eventType(),
                publishedEvent.envelope().aggregateId(),
                KafkaTopics.TRANSACTION_CREATED,
                publishedEvent.serializedPayload(),
                "PUBLISHED",
                Instant.now(clock)
        ));

        return new SubmitTransactionResult(transactionMapper.toSubmitResponse(transaction, true), true);
    }

    private static String validateChannel(String channel) {
        try {
            return Channel.valueOf(channel).name();
        } catch (RuntimeException ex) {
            throw new ApiException(400, ErrorCode.INVALID_CHANNEL, "Transaction channel is invalid");
        }
    }

    private static void validateCurrency(String currency) {
        if (!DEFAULT_CURRENCY.equals(currency)) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Only IDR currency is supported");
        }
    }

    private static void validateIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        try {
            InetAddress.getByName(ipAddress);
        } catch (Exception ex) {
            throw new ApiException(400, ErrorCode.INVALID_IP_ADDRESS, "IP address format is invalid");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
