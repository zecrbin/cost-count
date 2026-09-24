package com.costcount.common;

/** 账户流水类型和来源常量。 */
public interface TransactionConstant {

    /** 账户创建时记录的初始资金流水。 */
    String INITIAL = "INITIAL";
    /** 收入流水。 */
    String INCOME = "INCOME";
    /** 支出流水。 */
    String EXPENSE = "EXPENSE";
    /** 账户之间的转账流水。 */
    String TRANSFER = "TRANSFER";
    /** 不对应实际收支、仅用于校正余额的流水。 */
    String ADJUSTMENT = "ADJUSTMENT";

    /** 用户手工录入来源。 */
    String SOURCE_MANUAL = "MANUAL";
    /** 系统自动生成来源。 */
    String SOURCE_SYSTEM = "SYSTEM";
}
