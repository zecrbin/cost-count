package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * <p>{@code amount} 始终为业务金额正数，余额方向由 {@code balanceChange} 明确记录。</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("cc_transaction")
public class Transaction extends BaseEntity {

    /** 流水主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** INITIAL、INCOME、EXPENSE、TRANSFER 或 ADJUSTMENT。 */
    private String transactionType;
    /** 收入或支出分类 ID；初始化和余额调整流水可为空。修改流水类型时允许清空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long categoryId;
    /** 主账户；转账时表示转出账户。 */
    private Long accountId;
    /** 转账目标账户，非转账流水为空。修改流水类型时允许清空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long targetAccountId;
    /** 正数业务金额。 */
    private BigDecimal amount;
    /** 主账户余额实际变化，可正可负。 */
    private BigDecimal balanceChange;
    /** 主账户完成本笔流水后的余额快照。 */
    private BigDecimal balanceAfter;
    /** 业务发生时间。 */
    private LocalDateTime transactionTime;
    /** 交易对象或商户；更新时允许显式清空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String counterparty;
    /** MANUAL、IMPORT、AI 或 SYSTEM。 */
    private String source;
    /** 同一业务请求重试时复用的幂等号。 */
    private String requestId;
    /** 逻辑删除标识：0 未删除，1 已删除。 */
    @TableLogic
    private Integer isDeleted;
}
