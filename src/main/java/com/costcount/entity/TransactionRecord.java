package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("transaction_record")
public class TransactionRecord extends BaseEntity {
    private String type;
    private BigDecimal amount;
    private Long categoryId;
    private Long accountId;
    private Long targetAccountId;
    private LocalDate transactionDate;
    private String merchant;
    private String note;
    private String source;
}
