package com.costcount.service;

import com.costcount.entity.AccountDailyBalance;
import com.github.yulichang.base.MPJBaseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 账户日余额服务。
 */
public interface AccountDailyBalanceService extends MPJBaseService<AccountDailyBalance> {

    /**
     * 批量将末尾新增流水的余额变化追加到对应活动日快照。
     *
     * @param statDate 流水发生日期
     * @param changes 账户 ID 与本次余额变化
     * @param closingBalances 账户 ID 与流水入账后余额
     */
    void appendTransactionChanges(LocalDate statDate, Map<Long, BigDecimal> changes,
                                  Map<Long, BigDecimal> closingBalances);

    /** 查询指定日期之前最近活动日的期末余额，无历史快照时返回零。 */
    BigDecimal getClosingBalanceBefore(Long accountId, LocalDate statDate);

    /**
     * 从指定日期起重建账户的活动日余额；日期为空时重建全部记录。
     *
     * @param accountId 账户 ID
     * @param fromDate 重建起始日期（含）
     */
    void rebuildAccountDailyBalance(Long accountId, LocalDate fromDate);
}
