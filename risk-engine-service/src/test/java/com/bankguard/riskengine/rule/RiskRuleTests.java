package com.bankguard.riskengine.rule;

import static com.bankguard.riskengine.rule.RiskRuleTestSupport.config;
import static com.bankguard.riskengine.rule.RiskRuleTestSupport.context;
import static com.bankguard.riskengine.rule.RiskRuleTestSupport.transaction;
import static org.assertj.core.api.Assertions.assertThat;

import com.bankguard.riskengine.dto.RiskFactor;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RiskRuleTests {

    @Test
    void highAmountRuleShouldTriggerAtThresholdAndAboveOnly() {
        HighAmountRule rule = new HighAmountRule();
        var activeConfig = config(HighAmountRule.RULE_CODE, 25, "10000000");

        assertThat(rule.evaluate(context(transaction(new BigDecimal("9999999"), "DEVICE-1", "Jakarta"), activeConfig)).triggered())
                .isFalse();
        assertThat(rule.evaluate(context(transaction(new BigDecimal("10000000"), "DEVICE-1", "Jakarta"), activeConfig)).triggered())
                .isTrue();
        assertThat(rule.evaluate(context(transaction(new BigDecimal("25000000"), "DEVICE-1", "Jakarta"), activeConfig)).score())
                .isEqualTo(25);
    }

    @Test
    void newDeviceRuleShouldTriggerForUnknownOrUntrustedDevice() {
        NewDeviceRule rule = new NewDeviceRule();
        var activeConfig = config(NewDeviceRule.RULE_CODE, 20, null);
        var transaction = transaction(new BigDecimal("1000000"), "DEVICE-1", "Jakarta");

        assertThat(rule.evaluate(context(transaction, "LOW", true, true, true, false, 1, BigDecimal.ZERO, activeConfig)).triggered())
                .isFalse();
        assertThat(rule.evaluate(context(transaction, "LOW", false, true, true, false, 1, BigDecimal.ZERO, activeConfig)).triggered())
                .isTrue();
        assertThat(rule.evaluate(context(transaction, "LOW", false, false, true, false, 1, BigDecimal.ZERO, activeConfig)).triggered())
                .isTrue();
        assertThat(rule.evaluate(context(transaction(new BigDecimal("1000000"), null, "Jakarta"), activeConfig)).triggered())
                .isFalse();
    }

    @Test
    void blacklistedDestinationRuleShouldTriggerOnlyForActiveBlacklistMatch() {
        BlacklistedDestinationRule rule = new BlacklistedDestinationRule();
        var activeConfig = config(BlacklistedDestinationRule.RULE_CODE, 50, null);
        var transaction = transaction(new BigDecimal("1000000"), "DEVICE-1", "Jakarta");

        RiskFactor factor = rule.evaluate(context(transaction, "LOW", true, true, true, true, 1, BigDecimal.ZERO, activeConfig));

        assertThat(factor.triggered()).isTrue();
        assertThat(factor.score()).isEqualTo(50);
        assertThat(rule.evaluate(context(transaction, activeConfig)).triggered()).isFalse();
    }

    @Test
    void highFrequencyRuleShouldUseCountOrAmountVelocityThreshold() {
        HighFrequencyTransactionRule rule = new HighFrequencyTransactionRule();
        var countConfig = config(HighFrequencyTransactionRule.COUNT_RULE_CODE, 30, "5");
        var amountConfig = config(HighFrequencyTransactionRule.AMOUNT_RULE_CODE, 30, "50000000");
        var transaction = transaction(new BigDecimal("1000000"), "DEVICE-1", "Jakarta");

        assertThat(rule.evaluate(context(transaction, "LOW", true, true, true, false, 4, new BigDecimal("49999999"), countConfig, amountConfig)).triggered())
                .isFalse();
        assertThat(rule.evaluate(context(transaction, "LOW", true, true, true, false, 5, new BigDecimal("1000000"), countConfig, amountConfig)).triggered())
                .isTrue();
        assertThat(rule.evaluate(context(transaction, "LOW", true, true, true, false, 1, new BigDecimal("50000000"), countConfig, amountConfig)).triggered())
                .isTrue();
    }

    @Test
    void unusualLocationRuleShouldTriggerForUnknownLocationOnly() {
        UnusualLocationRule rule = new UnusualLocationRule();
        var activeConfig = config(UnusualLocationRule.RULE_CODE, 20, null);

        assertThat(rule.evaluate(context(transaction(new BigDecimal("1000000"), "DEVICE-1", "Jakarta"), activeConfig)).triggered())
                .isFalse();
        assertThat(rule.evaluate(context(
                transaction(new BigDecimal("1000000"), "DEVICE-1", "Bandung"),
                "LOW",
                true,
                true,
                false,
                false,
                1,
                BigDecimal.ZERO,
                activeConfig
        )).triggered()).isTrue();
        assertThat(rule.evaluate(context(transaction(new BigDecimal("1000000"), "DEVICE-1", null), activeConfig)).triggered())
                .isFalse();
    }

    @Test
    void highRiskCustomerProfileRuleShouldTriggerOnlyForHighRiskProfile() {
        HighRiskCustomerProfileRule rule = new HighRiskCustomerProfileRule();
        var activeConfig = config(HighRiskCustomerProfileRule.RULE_CODE, 25, null);
        var transaction = transaction(new BigDecimal("1000000"), "DEVICE-1", "Jakarta");

        assertThat(rule.evaluate(context(transaction, "LOW", true, true, true, false, 1, BigDecimal.ZERO, activeConfig)).triggered())
                .isFalse();
        assertThat(rule.evaluate(context(transaction, "MEDIUM", true, true, true, false, 1, BigDecimal.ZERO, activeConfig)).triggered())
                .isFalse();
        assertThat(rule.evaluate(context(transaction, "HIGH", true, true, true, false, 1, BigDecimal.ZERO, activeConfig)).triggered())
                .isTrue();
    }
}
