package com.bankguard.transaction.kafka;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.constant.EventType;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.constant.ProducerName;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.EventEnvelopeFactory;
import com.bankguard.common.event.TransactionCreatedPayload;
import com.bankguard.common.exception.ApiException;
import com.bankguard.transaction.entity.TransactionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionCreatedEventPublisher {
    private final KafkaTemplate<String, EventEnvelope<TransactionCreatedPayload>> kafkaTemplate;
    private final EventEnvelopeFactory eventEnvelopeFactory;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public TransactionCreatedEventPublisher(
            KafkaTemplate<String, EventEnvelope<TransactionCreatedPayload>> kafkaTemplate,
            EventEnvelopeFactory eventEnvelopeFactory
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventEnvelopeFactory = eventEnvelopeFactory;
    }

    public PublishedTransactionEvent publishCreated(TransactionEntity transaction, String requestId) {
        TransactionCreatedPayload payload = new TransactionCreatedPayload(
                transaction.getTransactionRef(),
                transaction.getSourceAccount().getAccountNumber(),
                transaction.getDestinationAccountNumber(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getChannel(),
                transaction.getDeviceId(),
                transaction.getIpAddress(),
                transaction.getLocation(),
                transaction.getCreatedAt()
        );
        EventEnvelope<TransactionCreatedPayload> envelope = eventEnvelopeFactory.transactionEvent(
                EventType.TRANSACTION_CREATED.name(),
                transaction.getTransactionRef(),
                ProducerName.TRANSACTION_SERVICE,
                requestId,
                requestId,
                payload
        );

        try {
            kafkaTemplate.send(KafkaTopics.TRANSACTION_CREATED, transaction.getTransactionRef(), envelope).get(10, TimeUnit.SECONDS);
            return new PublishedTransactionEvent(envelope, objectMapper.writeValueAsString(envelope));
        } catch (Exception ex) {
            throw new ApiException(500, ErrorCode.EVENT_PUBLISH_FAILED, "Failed to publish transaction.created event");
        }
    }
}
