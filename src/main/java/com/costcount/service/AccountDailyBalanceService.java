package com.costcount.service;

import java.time.LocalDate;

/**
 * 账户每日余额汇总服务。
 *
 * <p>每日汇总是可重建缓存，不是账务事实；事实数据始终来自交易流水。</p>
 */
public interface AccountDailyBalanceService {

    /** 从指定业务日期开始重建账户的每日余额快照。 */
    void rebuildFrom(Long accountId, LocalDate fromDate);
}
