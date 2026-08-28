package com.costcount.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountSummaryVO {

    /**
     * 所有 DEBIT 账户余额合计
     */
    private BigDecimal totalAsset;

    /**
     * 所有 CREDIT 账户待还金额合计
     */
    private BigDecimal totalLiability;

    /**
     * 总资产 - 总负债
     */
    private BigDecimal netAsset;

    /**
     * 所有信用账户授信额度
     */
    private BigDecimal totalCreditLimit;

    /**
     * 信用额度整体使用率
     */
    private BigDecimal creditUsageRate;
}