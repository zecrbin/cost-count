package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账户每日余额汇总缓存。
 *
 * <p>该表没有逻辑删除字段，因为记录可以从流水事实重新生成；修正时直接更新或重建即可。</p>
 */
@Data
@TableName("cc_account_daily_balance")
public class AccountDailyBalance {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 汇总所属账户。 */
    private Long accountId;
    /** 自然日。 */
    private LocalDate statDate;
    /** 当日开始余额。 */
    private BigDecimal openingBalance;
    /** 当日已入账流水的净变化。 */
    private BigDecimal transactionChange;
    /** 历史补录或重算产生的修正变化。 */
    private BigDecimal correctionChange;
    /** 当日结束余额。 */
    private BigDecimal closingBalance;
    /** 最近一次重建时间。 */
    private LocalDateTime rebuiltTime;
}
