package com.costcount.service;

import com.costcount.entity.AccountDailyBalance;
import com.costcount.entity.Transaction;
import com.github.yulichang.base.MPJBaseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 账户日余额服务。
 */
public interface AccountDailyBalanceService extends MPJBaseService<AccountDailyBalance> {

    /** 查询指定日期之前最近活动日的期末余额，无历史快照时返回零。 */
    BigDecimal getClosingBalanceBefore(Long accountId, LocalDate statDate);

    /** 同上；调用方已经知道账户是否为信用类时传入，省掉一次账户和账户类型查询。 */
    BigDecimal getClosingBalanceBefore(Long accountId, LocalDate statDate, boolean credit);

    /**
     * 把一笔末尾流水的余额变化追加到发生日快照。
     *
     * <p>只适用于账户最新的一笔流水：发生日即最后一个活动日，前面的快照都不受影响。
     * 插入历史区间的补充流水必须走 {@link #rebuildDailyBalances}。</p>
     *
     * @param change 本笔流水对该账户的余额变化
     * @param correction 是否为余额调整；所有变化都计入当日变动，余额调整另在修正列记一份
     * @param closingBalance 本笔流水入账后的账户余额
     */
    void appendDailyBalance(Long accountId, LocalDate statDate, BigDecimal change, boolean correction,
                            BigDecimal closingBalance);

    /**
     * 从指定日期起重建账户的活动日余额；日期为空时重建全部记录。
     *
     * <p>自行对账户加行锁，可作为独立的修复入口调用。</p>
     *
     * @param accountId 账户 ID
     * @param fromDate 重建起始日期（含）
     */
    void rebuildAccountDailyBalance(Long accountId, LocalDate fromDate);

    /**
     * 用调用方已经取好的流水重建活动日快照。
     *
     * <p>供记账链路复用同一份流水，避免重新查询账户性质、期初锚点和流水列表。
     * 调用方必须已持有账户行锁，{@code transactions} 必须按 (transaction_time, id) 升序
     * 且覆盖 {@code fromDate} 起的全部流水。</p>
     *
     * @param credit 账户是否为信用类，决定转入方向的反推
     * @param openingBalance {@code fromDate} 当日的期初余额
     */
    void rebuildDailyBalances(Long accountId, LocalDate fromDate, boolean credit,
                              BigDecimal openingBalance, List<Transaction> transactions);
}
