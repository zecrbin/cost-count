package com.costcount.enums;

import java.util.Set;

public enum AccountNature {
    ASSET, LIABILITY;

    private static final Set<String> LIABILITY_TYPES = Set.of("花呗", "招商银行信用卡");

    public static AccountNature fromAccountType(String accountType) {
        if (LIABILITY_TYPES.contains(accountType) || accountType != null && accountType.contains("信用卡")) {
            return LIABILITY;
        }
        return ASSET;
    }
}
