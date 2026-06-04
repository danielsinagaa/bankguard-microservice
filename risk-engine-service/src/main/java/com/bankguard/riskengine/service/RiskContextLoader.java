package com.bankguard.riskengine.service;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.common.redis.RedisKeyBuilder;
import com.bankguard.riskengine.cache.VelocityCounterService;
import com.bankguard.riskengine.cache.VelocitySnapshot;
import com.bankguard.riskengine.dto.RuleConfig;
import com.bankguard.riskengine.dto.TransactionContext;
import com.bankguard.riskengine.entity.CustomerDeviceEntity;
import com.bankguard.riskengine.entity.TransactionEntity;
import com.bankguard.riskengine.repository.BlacklistedAccountRepository;
import com.bankguard.riskengine.repository.CustomerDeviceRepository;
import com.bankguard.riskengine.repository.CustomerLocationRepository;
import com.bankguard.riskengine.repository.CustomerRiskProfileRepository;
import com.bankguard.riskengine.repository.RiskRuleConfigRepository;
import com.bankguard.riskengine.repository.TransactionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskContextLoader {
    private static final Logger log = LoggerFactory.getLogger(RiskContextLoader.class);
    private static final Duration BLACKLIST_TTL = Duration.ofMinutes(10);
    private static final Duration BLACKLIST_NEGATIVE_TTL = Duration.ofMinutes(1);
    private static final Duration RISK_PROFILE_TTL = Duration.ofMinutes(5);
    private static final Duration TRUSTED_DEVICE_TTL = Duration.ofMinutes(10);
    private static final Duration RULE_CONFIG_TTL = Duration.ofMinutes(5);
    private static final List<String> RULE_CONFIG_CODES = List.of(
            "HIGH_AMOUNT",
            "NEW_DEVICE",
            "BLACKLISTED_DESTINATION",
            "HIGH_FREQUENCY_TRANSACTION_COUNT",
            "HIGH_FREQUENCY_TRANSACTION_AMOUNT",
            "UNUSUAL_LOCATION",
            "HIGH_RISK_CUSTOMER_PROFILE"
    );

    private final TransactionRepository transactionRepository;
    private final CustomerRiskProfileRepository riskProfileRepository;
    private final CustomerDeviceRepository deviceRepository;
    private final CustomerLocationRepository locationRepository;
    private final BlacklistedAccountRepository blacklistedAccountRepository;
    private final RiskRuleConfigRepository riskRuleConfigRepository;
    private final VelocityCounterService velocityCounterService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public RiskContextLoader(
            TransactionRepository transactionRepository,
            CustomerRiskProfileRepository riskProfileRepository,
            CustomerDeviceRepository deviceRepository,
            CustomerLocationRepository locationRepository,
            BlacklistedAccountRepository blacklistedAccountRepository,
            RiskRuleConfigRepository riskRuleConfigRepository,
            VelocityCounterService velocityCounterService,
            StringRedisTemplate redisTemplate
    ) {
        this.transactionRepository = transactionRepository;
        this.riskProfileRepository = riskProfileRepository;
        this.deviceRepository = deviceRepository;
        this.locationRepository = locationRepository;
        this.blacklistedAccountRepository = blacklistedAccountRepository;
        this.riskRuleConfigRepository = riskRuleConfigRepository;
        this.velocityCounterService = velocityCounterService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(readOnly = true)
    public TransactionContext load(String transactionRef) {
        TransactionEntity transaction = transactionRepository.findContextByTransactionRef(transactionRef)
                .orElseThrow(() -> new ApiException(404, ErrorCode.TRANSACTION_NOT_FOUND, "Transaction does not exist"));
        Long customerId = transaction.getSourceAccount().getCustomer().getId();
        String deviceId = transaction.getDeviceId();
        DeviceContext device = loadDevice(customerId, deviceId);
        boolean knownLocation = transaction.getLocation() != null
                && !transaction.getLocation().isBlank()
                && locationRepository.existsByCustomerIdAndLocation(customerId, transaction.getLocation());
        boolean blacklisted = isBlacklisted(transaction.getDestinationAccountNumber());
        VelocitySnapshot velocity = velocityCounterService.incrementAndGet(
                transaction.getSourceAccount().getAccountNumber(),
                transaction.getAmount()
        );

        Map<String, RuleConfig> configs = loadRuleConfigs();

        return new TransactionContext(
                transaction,
                loadRiskLevel(customerId),
                device.trusted(),
                device.known(),
                knownLocation,
                blacklisted,
                velocity.count(),
                velocity.amount(),
                configs
        );
    }

    private String loadRiskLevel(Long customerId) {
        String key = RedisKeyBuilder.customerRiskProfile(customerId);
        Optional<String> cachedRiskLevel = readCache(key)
                .map(value -> textValue(value.get("riskLevel")))
                .filter(value -> !value.isBlank());
        if (cachedRiskLevel.isPresent()) {
            return cachedRiskLevel.get();
        }

        Optional<String> databaseRiskLevel = riskProfileRepository.findByCustomerId(customerId)
                .map(profile -> profile.getRiskLevel());
        databaseRiskLevel.ifPresent(riskLevel -> {
            writeCache(key, mapOf(
                    "customerId", customerId,
                    "riskLevel", riskLevel
            ), RISK_PROFILE_TTL);
        });
        return databaseRiskLevel.orElse("LOW");
    }

    private DeviceContext loadDevice(Long customerId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return new DeviceContext(false, false);
        }

        String key = RedisKeyBuilder.trustedDevice(customerId, deviceId);
        Optional<Boolean> cachedTrusted = readCache(key)
                .map(value -> booleanValue(value.get("trusted")));
        if (cachedTrusted.isPresent()) {
            return new DeviceContext(true, cachedTrusted.get());
        }

        Optional<CustomerDeviceEntity> device = deviceRepository.findByCustomerIdAndDeviceId(customerId, deviceId);
        device.ifPresent(value -> writeCache(key, mapOf(
                "customerId", customerId,
                "deviceId", deviceId,
                "trusted", value.isTrusted()
        ), TRUSTED_DEVICE_TTL));
        return new DeviceContext(device.isPresent(), device.map(CustomerDeviceEntity::isTrusted).orElse(false));
    }

    private boolean isBlacklisted(String accountNumber) {
        String key = RedisKeyBuilder.blacklistAccount(accountNumber);
        Optional<Boolean> cachedActive = readCache(key)
                .map(value -> booleanValue(value.get("active")));
        if (cachedActive.isPresent()) {
            return cachedActive.get();
        }
        Optional<Boolean> negativeCacheHit = readCache(RedisKeyBuilder.blacklistAccountNegative(accountNumber))
                .map(value -> booleanValue(value.get("active")));
        if (negativeCacheHit.isPresent()) {
            return false;
        }

        boolean active = blacklistedAccountRepository.existsByAccountNumberAndActiveTrue(accountNumber);
        if (active) {
            writeCache(key, mapOf(
                    "accountNumber", accountNumber,
                    "active", true
            ), BLACKLIST_TTL);
        } else {
            writeCache(RedisKeyBuilder.blacklistAccountNegative(accountNumber), mapOf(
                    "accountNumber", accountNumber,
                    "active", false
            ), BLACKLIST_NEGATIVE_TTL);
        }
        return active;
    }

    private Map<String, RuleConfig> loadRuleConfigs() {
        Map<String, RuleConfig> cachedConfigs = RULE_CONFIG_CODES.stream()
                .map(this::readRuleConfig)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(RuleConfig::ruleCode, Function.identity()));
        if (cachedConfigs.size() == RULE_CONFIG_CODES.size()) {
            return cachedConfigs;
        }

        return riskRuleConfigRepository.findByActiveTrue().stream()
                .map(config -> new RuleConfig(config.getRuleCode(), config.getScore(), config.getThresholdValue(), config.isActive()))
                .peek(this::cacheRuleConfig)
                .collect(Collectors.toMap(RuleConfig::ruleCode, Function.identity()));
    }

    private Optional<RuleConfig> readRuleConfig(String ruleCode) {
        String key = RedisKeyBuilder.riskRuleConfig(ruleCode);
        return readCache(key).map(value -> new RuleConfig(
                textValue(value.get("ruleCode")),
                intValue(value.get("score")),
                decimalValue(value.get("thresholdValue")),
                booleanValue(value.get("active"))
        ));
    }

    private void cacheRuleConfig(RuleConfig config) {
        writeCache(RedisKeyBuilder.riskRuleConfig(config.ruleCode()), mapOf(
                "ruleCode", config.ruleCode(),
                "score", config.score(),
                "thresholdValue", config.thresholdValue(),
                "active", config.active()
        ), RULE_CONFIG_TTL);
    }

    private Optional<Map<String, Object>> readCache(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, new TypeReference<>() {
            }));
        } catch (RuntimeException | java.io.IOException ex) {
            log.warn("Redis cache read failed, key={}, fallback=postgresql, error={}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private void writeCache(String key, Map<String, Object> value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (RuntimeException | java.io.IOException ex) {
            log.warn("Redis cache write failed, key={}, fallback=postgresql, error={}", key, ex.getMessage());
        }
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(values[i].toString(), values[i + 1]);
        }
        return map;
    }

    private String textValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString());
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private record DeviceContext(boolean known, boolean trusted) {
    }
}
