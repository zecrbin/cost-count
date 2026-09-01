package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.costcount.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户流水实体。
 *
 * <p>{@code amount} 始终为业务金额正数，余额方向由 {@code balanceChange} 和
 * {@code targetBalanceChange} 明确记录，从而支持资产账户和信用账户的不同语义。</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("cc_transaction")
public class Transaction extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** INITIAL、INCOME、EXPENSE、TRANSFER 或 ADJUSTMENT。 */
    private String transactionType;
    /** 收入/支出分类；转账、初始和调整流水为空。 */
    private Long categoryId;
    /** 主账户；转账时表示转出账户。 */
    private Long accountId;
    /** 转账目标账户，非转账流水为空。 */
    private Long targetAccountId;
    /** 正数业务金额。 */
    private BigDecimal amount;
    /** 主账户余额实际变化，可正可负。 */
    private BigDecimal balanceChange;
    /** 主账户完成本笔流水后的余额快照。 */
    private BigDecimal balanceAfter;
    /** 目标账户余额实际变化，仅转账使用。 */
    private BigDecimal targetBalanceChange;
    /** 目标账户完成本笔转账后的余额快照。 */
    private BigDecimal targetBalanceAfter;
    /** 业务发生时间，补录时可早于入账时间。 */
    private LocalDateTime transactionTime;
    /** 实际写入账本的时间。 */
    private LocalDateTime postedTime;
    /** 交易对象或商户。 */
    private String counterparty;
    /** MANUAL、IMPORT、AI 或 SYSTEM。 */
    private String source;
    /** 同一业务请求重试时复用的幂等号。 */
    private String requestId;
    /** ADJUSTMENT 的业务原因。 */
    private String adjustmentReason;
    /** 冲正流水对应的原流水 ID。 */
    private Long reversalOfId;
    /** 0草稿、1已入账、2已冲正。 */
    private Integer status;

    @TableLogic
    private Integer isDeleted;
}
