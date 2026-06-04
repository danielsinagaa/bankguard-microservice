package com.bankguard.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.TransactionCreatedPayload;
import com.bankguard.common.exception.ApiException;
import com.bankguard.transaction.dto.request.SubmitTransactionRequest;
import com.bankguard.transaction.entity.AccountEntity;
import com.bankguard.transaction.entity.CustomerEntity;
import com.bankguard.transaction.entity.KafkaEventLogEntity;
import com.bankguard.transaction.entity.TransactionEntity;
import com.bankguard.transaction.kafka.PublishedTransactionEvent;
import com.bankguard.transaction.kafka.TransactionCreatedEventPublisher;
import com.bankguard.transaction.mapper.TransactionMapper;
import com.bankguard.transaction.repository.KafkaEventLogRepository;
import com.bankguard.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class TransactionServiceTest {
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final KafkaEventLogRepository kafkaEventLogRepository = mock(KafkaEventLogRepository.class);
    private final AccountValidationService accountValidationService = mock(AccountValidationService.class);
    private final TransactionReferenceGenerator referenceGenerator = mock(TransactionReferenceGenerator.class);
    private final TransactionCreatedEventPublisher eventPublisher = mock(TransactionCreatedEventPublisher.class);
    private final TransactionMapper mapper = new TransactionMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC);
    private final TransactionService service = new TransactionService(
            transactionRepository,
            kafkaEventLogRepository,
            accountValidationService,
            referenceGenerator,
            eventPublisher,
            mapper,
            clock
    );

    @Test
    void shouldRequireIdempotencyKey() {
        assertThatThrownBy(() -> service.submit(request(), null, "req-001"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    void shouldReturnExistingTransactionForDuplicateIdempotencyKey() {
        TransactionEntity existing = transaction();
        when(transactionRepository.findByIdempotencyKey("idem-001")).thenReturn(Optional.of(existing));

        SubmitTransactionResult result = service.submit(request(), "idem-001", "req-001");

        assertThat(result.created()).isFalse();
        assertThat(result.response().transactionRef()).isEqualTo("TRX-20260604-000001");
        verify(eventPublisher, never()).publishCreated(any(), any());
    }

    @Test
    void shouldCreateTransactionPublishEventAndRecordEventLog() {
        AccountEntity sourceAccount = sourceAccount();
        when(transactionRepository.findByIdempotencyKey("idem-001")).thenReturn(Optional.empty());
        when(accountValidationService.validateSourceAccount("1234567890")).thenReturn(sourceAccount);
        when(referenceGenerator.generate()).thenReturn("TRX-20260604-000001");
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventPublisher.publishCreated(any(TransactionEntity.class), org.mockito.ArgumentMatchers.eq("req-001")))
                .thenReturn(publishedEvent());

        SubmitTransactionResult result = service.submit(request(), "idem-001", "req-001");

        assertThat(result.created()).isTrue();
        assertThat(result.response().status()).isEqualTo("PENDING_RISK_CHECK");
        verify(eventPublisher).publishCreated(any(TransactionEntity.class), org.mockito.ArgumentMatchers.eq("req-001"));
        verify(kafkaEventLogRepository).save(any(KafkaEventLogEntity.class));
    }

    @Test
    void shouldRejectInvalidChannel() {
        SubmitTransactionRequest request = new SubmitTransactionRequest(
                "1234567890",
                "9876543210",
                BigDecimal.TEN,
                "IDR",
                "WHATSAPP_BANKING",
                null,
                null,
                null
        );
        when(transactionRepository.findByIdempotencyKey("idem-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(request, "idem-001", "req-001"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CHANNEL);
    }

    private static SubmitTransactionRequest request() {
        return new SubmitTransactionRequest(
                "1234567890",
                "9876543210",
                BigDecimal.valueOf(25000000),
                "IDR",
                "MOBILE_BANKING",
                "DEVICE-001",
                "36.77.88.12",
                "Jakarta"
        );
    }

    private static TransactionEntity transaction() {
        return new TransactionEntity(
                "TRX-20260604-000001",
                "idem-001",
                sourceAccount(),
                "9876543210",
                BigDecimal.valueOf(25000000),
                "IDR",
                "MOBILE_BANKING",
                "DEVICE-001",
                "36.77.88.12",
                "Jakarta",
                "PENDING_RISK_CHECK",
                Instant.parse("2026-06-04T10:00:00Z")
        );
    }

    private static AccountEntity sourceAccount() {
        CustomerEntity customer = new CustomerEntity("CIF001", "Daniel Sinaga", "daniel@example.com", "0811", "ACTIVE");
        return new AccountEntity(customer, "1234567890", "SAVINGS", "IDR", BigDecimal.TEN, "ACTIVE");
    }

    private static PublishedTransactionEvent publishedEvent() {
        EventEnvelope<TransactionCreatedPayload> envelope = new EventEnvelope<>(
                "7a7f0e9b-3dd6-4f22-b23a-44e42c25a001",
                "TRANSACTION_CREATED",
                "1.0",
                "TRANSACTION",
                "TRX-20260604-000001",
                Instant.parse("2026-06-04T10:00:00Z"),
                "transaction-service",
                "req-001",
                "req-001",
                new TransactionCreatedPayload(
                        "TRX-20260604-000001",
                        "1234567890",
                        "9876543210",
                        BigDecimal.valueOf(25000000),
                        "IDR",
                        "MOBILE_BANKING",
                        "DEVICE-001",
                        "36.77.88.12",
                        "Jakarta",
                        Instant.parse("2026-06-04T10:00:00Z")
                )
        );
        return new PublishedTransactionEvent(envelope, "{\"eventType\":\"TRANSACTION_CREATED\"}");
    }
}
