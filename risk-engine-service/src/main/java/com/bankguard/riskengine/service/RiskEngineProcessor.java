package com.bankguard.riskengine.service;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.constant.EventType;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.TransactionCreatedPayload;
import com.bankguard.common.exception.ApiException;
import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.RiskScoringResult;
import com.bankguard.riskengine.dto.TransactionContext;
import com.bankguard.riskengine.entity.KafkaEventLogEntity;
import com.bankguard.riskengine.entity.TransactionEntity;
import com.bankguard.riskengine.entity.TransactionRiskFactorEntity;
import com.bankguard.riskengine.kafka.PublishedRiskScoredEvent;
import com.bankguard.riskengine.kafka.RiskScoredEventPublisher;
import com.bankguard.riskengine.repository.KafkaEventLogRepository;
import com.bankguard.riskengine.repository.TransactionRepository;
import com.bankguard.riskengine.repository.TransactionRiskFactorRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskEngineProcessor {
    public static final String CONSUMER_GROUP = "risk-engine-service-group";

    private final KafkaEventLogRepository kafkaEventLogRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionRiskFactorRepository riskFactorRepository;
    private final RiskContextLoader riskContextLoader;
    private final RiskScoringService riskScoringService;
    private final RiskScoredEventPublisher riskScoredEventPublisher;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public RiskEngineProcessor(
            KafkaEventLogRepository kafkaEventLogRepository,
            TransactionRepository transactionRepository,
            TransactionRiskFactorRepository riskFactorRepository,
            RiskContextLoader riskContextLoader,
            RiskScoringService riskScoringService,
            RiskScoredEventPublisher riskScoredEventPublisher,
            Clock clock
    ) {
        this.kafkaEventLogRepository = kafkaEventLogRepository;
        this.transactionRepository = transactionRepository;
        this.riskFactorRepository = riskFactorRepository;
        this.riskContextLoader = riskContextLoader;
        this.riskScoringService = riskScoringService;
        this.riskScoredEventPublisher = riskScoredEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void process(EventEnvelope<TransactionCreatedPayload> envelope, String serializedPayload) {
        if (kafkaEventLogRepository.existsByEventIdAndConsumerGroup(envelope.eventId(), CONSUMER_GROUP)) {
            return;
        }

        TransactionEntity existingTransaction = transactionRepository.findContextByTransactionRef(envelope.payload().transactionRef())
                .orElseThrow(() -> new ApiException(404, ErrorCode.TRANSACTION_NOT_FOUND, "Transaction does not exist"));
        Instant now = Instant.now(clock);
        if (existingTransaction.isFinalDecision()) {
            kafkaEventLogRepository.save(eventLog(envelope, serializedPayload, "IGNORED", now));
            return;
        }

        TransactionContext context = riskContextLoader.load(envelope.payload().transactionRef());
        RiskScoringResult result = riskScoringService.score(context);
        Instant scoredAt = Instant.now(clock);
        TransactionEntity transaction = context.transaction();

        result.riskFactors().stream()
                .map(factor -> toEntity(transaction, factor, scoredAt))
                .forEach(riskFactorRepository::save);
        transaction.applyRiskResult(result.totalScore(), result.decision(), scoredAt);
        transactionRepository.save(transaction);
        kafkaEventLogRepository.save(eventLog(envelope, serializedPayload, "CONSUMED", scoredAt));

        PublishedRiskScoredEvent publishedEvent = riskScoredEventPublisher.publish(
                transaction,
                result,
                envelope.requestId(),
                scoredAt
        );
        kafkaEventLogRepository.save(new KafkaEventLogEntity(
                publishedEvent.envelope().eventId(),
                EventType.TRANSACTION_RISK_SCORED.name(),
                publishedEvent.envelope().aggregateId(),
                KafkaTopics.TRANSACTION_RISK_SCORED,
                null,
                publishedEvent.serializedPayload(),
                "PUBLISHED",
                scoredAt,
                null
        ));
    }

    private KafkaEventLogEntity eventLog(
            EventEnvelope<TransactionCreatedPayload> envelope,
            String serializedPayload,
            String status,
            Instant processedAt
    ) {
        return new KafkaEventLogEntity(
                envelope.eventId(),
                envelope.eventType(),
                envelope.aggregateId(),
                KafkaTopics.TRANSACTION_CREATED,
                CONSUMER_GROUP,
                serializedPayload,
                status,
                Instant.now(clock),
                processedAt
        );
    }

    private TransactionRiskFactorEntity toEntity(TransactionEntity transaction, RiskFactor factor, Instant createdAt) {
        return new TransactionRiskFactorEntity(
                transaction,
                factor.code(),
                factor.description(),
                factor.score(),
                serializeMetadata(factor),
                createdAt
        );
    }

    private String serializeMetadata(RiskFactor factor) {
        try {
            return objectMapper.writeValueAsString(factor.metadata());
        } catch (JsonProcessingException ex) {
            throw new ApiException(500, ErrorCode.INTERNAL_SERVER_ERROR, "Failed to serialize risk factor metadata");
        }
    }
}
