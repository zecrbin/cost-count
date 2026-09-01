package com.costcount.common;

public interface CommonConstant {
    String DEBIT = "DEBIT";
    String CREDIT = "CREDIT";

    String TRANSACTION_INITIAL = "INITIAL";
    String TRANSACTION_INCOME = "INCOME";
    String TRANSACTION_EXPENSE = "EXPENSE";
    String TRANSACTION_TRANSFER = "TRANSFER";
    String TRANSACTION_ADJUSTMENT = "ADJUSTMENT";

    String SOURCE_MANUAL = "MANUAL";
    String SOURCE_IMPORT = "IMPORT";
    String SOURCE_AI = "AI";
    String SOURCE_SYSTEM = "SYSTEM";

    Integer TRANSACTION_DRAFT = 0;
    Integer TRANSACTION_POSTED = 1;
    Integer TRANSACTION_REVERSED = 2;

    Integer DEFAULT_SORT = 0;

    Integer NORMAL_STATUS = 1;

    Integer DELETED = 1;
    Integer NOT_DELETED = 0;

    Integer IS_DEFAULT_CODE = 1;

}
