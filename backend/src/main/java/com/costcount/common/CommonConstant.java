package com.costcount.common;

/** 全局通用默认值和状态常量。 */
public interface CommonConstant {

    /** 默认展示顺序。 */
    Integer DEFAULT_SORT = 0;

    /** 停用状态。 */
    Integer STATUS_DISABLED = 0;

    /** 启用状态。 */
    Integer STATUS_ENABLED = 1;

    /** 账户类型：存储账户。 */
    String DEBIT_ACCOUNT_TYPE = "DEBIT";

    /** 账户类型：信用账户。 */
    String CREDIT_ACCOUNT_TYPE = "CREDIT";

    /**
     * 判断账户类型编码是否为信用类。
     *
     * <p>type_code 由用户录入且入库时不统一大小写，各处必须共用这一个判定，
     * 否则同一个账户在不同链路上会被算成不同性质，转入方向的正负号会对不上。</p>
     */
    static boolean isCreditType(String typeCode) {
        return CREDIT_ACCOUNT_TYPE.equalsIgnoreCase(typeCode);
    }
}
