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

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("cc_transaction")
public class Transaction extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String transactionType;
    private Long categoryId;
    private Long accountId;
    private Long targetAccountId;
    private BigDecimal amount;
    private BigDecimal balanceChange;
    private BigDecimal balanceAfter;
    private BigDecimal targetBalanceChange;
    private BigDecimal targetBalanceAfter;
    private LocalDateTime transactionTime;
    private LocalDateTime postedTime;
    private String counterparty;
    private String source;
    private String requestId;
    private String adjustmentReason;
    private Integer status;

    @TableLogic
    private Integer isDeleted;
}
