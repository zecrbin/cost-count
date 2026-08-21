package com.costcount.enums;

import com.costcount.exception.BizException;

import java.util.Arrays;

public enum TransactionType {
    INCOME, EXPENSE;

    public static TransactionType of(String value) {
        return Arrays.stream(values())
            .filter(item -> item.name().equals(value))
            .findFirst()
            .orElseThrow(() -> new BizException(400, "收支类型仅支持 INCOME 或 EXPENSE"));
    }
}
