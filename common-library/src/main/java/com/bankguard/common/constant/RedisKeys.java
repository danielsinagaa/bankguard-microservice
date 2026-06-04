package com.bankguard.common.constant;

public final class RedisKeys {
    public static final String BLACKLIST_ACCOUNT_PREFIX = "blacklist:account";
    public static final String CUSTOMER_RISK_PROFILE_PREFIX = "customer:risk-profile";
    public static final String CUSTOMER_TRUSTED_DEVICE_PREFIX = "customer:trusted-device";
    public static final String RISK_RULE_CONFIG_PREFIX = "risk-rule:config";
    public static final String VELOCITY_COUNT_PREFIX = "risk:velocity:count";
    public static final String VELOCITY_AMOUNT_PREFIX = "risk:velocity:amount";

    private RedisKeys() {
    }
}
