package com.bankguard.common.redis;

import com.bankguard.common.constant.RedisKeys;

public final class RedisKeyBuilder {
    private static final String TEN_MINUTE_WINDOW = "10m";

    private RedisKeyBuilder() {
    }

    public static String blacklistAccount(String accountNumber) {
        return RedisKeys.BLACKLIST_ACCOUNT_PREFIX + ":" + required(accountNumber, "accountNumber");
    }

    public static String blacklistAccountNegative(String accountNumber) {
        return blacklistAccount(accountNumber) + ":negative";
    }

    public static String customerRiskProfile(Long customerId) {
        return RedisKeys.CUSTOMER_RISK_PROFILE_PREFIX + ":" + required(customerId, "customerId");
    }

    public static String trustedDevice(Long customerId, String deviceId) {
        return RedisKeys.CUSTOMER_TRUSTED_DEVICE_PREFIX
                + ":" + required(customerId, "customerId")
                + ":" + required(deviceId, "deviceId");
    }

    public static String riskRuleConfig(String ruleCode) {
        return RedisKeys.RISK_RULE_CONFIG_PREFIX + ":" + required(ruleCode, "ruleCode");
    }

    public static String velocityCount(String sourceAccountNumber) {
        return RedisKeys.VELOCITY_COUNT_PREFIX + ":" + required(sourceAccountNumber, "sourceAccountNumber") + ":" + TEN_MINUTE_WINDOW;
    }

    public static String velocityAmount(String sourceAccountNumber) {
        return RedisKeys.VELOCITY_AMOUNT_PREFIX + ":" + required(sourceAccountNumber, "sourceAccountNumber") + ":" + TEN_MINUTE_WINDOW;
    }

    private static String required(Object value, String fieldName) {
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.toString();
    }
}
