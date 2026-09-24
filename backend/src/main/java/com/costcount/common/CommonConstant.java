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

    /** 根分类的父分类 ID。 */
    Long ROOT_CATEGORY_PID = 0L;
}
