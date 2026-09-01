package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("cc_account_daily_balance")
public class AccountDailyBalance {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long accountId;
    private LocalDate statDate;
    private BigDecimal openingBalance;
    private BigDecimal transactionChange;
    private BigDecimal correctionChange;
    private BigDecimal closingBalance;
    private LocalDateTime rebuiltTime;
}
