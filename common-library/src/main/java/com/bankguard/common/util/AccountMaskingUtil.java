package com.bankguard.common.util;

public final class AccountMaskingUtil {
    private static final int LONG_ACCOUNT_MIN_LENGTH = 7;
    private static final int VISIBLE_PREFIX_LENGTH = 3;
    private static final int VISIBLE_SUFFIX_LENGTH = 3;
    private static final int SHORT_VISIBLE_SUFFIX_LENGTH = 2;
    private static final String MASK = "****";

    private AccountMaskingUtil() {
    }

    public static String mask(String accountNumber) {
        if (accountNumber == null) {
            return null;
        }

        if (accountNumber.isBlank()) {
            return "";
        }

        if (accountNumber.length() >= LONG_ACCOUNT_MIN_LENGTH) {
            return accountNumber.substring(0, VISIBLE_PREFIX_LENGTH)
                    + MASK
                    + accountNumber.substring(accountNumber.length() - VISIBLE_SUFFIX_LENGTH);
        }

        if (accountNumber.length() <= SHORT_VISIBLE_SUFFIX_LENGTH) {
            return accountNumber;
        }

        return MASK + accountNumber.substring(accountNumber.length() - SHORT_VISIBLE_SUFFIX_LENGTH);
    }
}
