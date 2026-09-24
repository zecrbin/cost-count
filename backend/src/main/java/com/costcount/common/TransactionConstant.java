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
    /**
     * 余额调整，修正账面与实际余额的小额偏差，典型场景是利率变动后仍按旧利率计算造成的利息差额。
     * 正常的利息记为收入，不用此类型。余额修正接口记账时也使用此类型。
     */
    String ADJUSTMENT = "ADJUSTMENT";

    /** 用户手工录入来源。 */
    String SOURCE_MANUAL = "MANUAL";
    /** 系统自动生成来源。 */
    String SOURCE_SYSTEM = "SYSTEM";
}
