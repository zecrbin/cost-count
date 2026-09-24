package com.costcount.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.costcount.common.PageQuery;
import com.costcount.dto.transaction.InitialTransactionSaveDTO;
import com.costcount.dto.transaction.TransactionQueryDTO;
import com.costcount.dto.transaction.TransactionSaveDTO;
import com.costcount.entity.Transaction;
import com.costcount.vo.transaction.TransactionVO;
import com.github.yulichang.base.MPJBaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 交易流水服务。
 */
public interface TransactionService extends MPJBaseService<Transaction> {

    Page<TransactionVO> pageQueryTransactions(PageQuery<TransactionQueryDTO> pageQuery);

    void saveInitialTransaction(@Valid InitialTransactionSaveDTO initialTransactionSaveDTO);

    /**
     * 录入流水，按发生时间自动选择记账方式，调用方无需区分。
     *
     * <p>对每个相关账户分别判断：不早于该账户最近一笔流水时直接追加余额和发生日快照；
     * 早于它则说明插入了历史区间，从发生日起重放该账户的流水并重建快照。</p>
     */
    String saveTransaction(@Valid TransactionSaveDTO transactionSaveDTO);

    /**
     * 把账户余额校正到指定值：差额以一笔当前时间的调整流水记账。
     *
     * @return 调整流水 ID；余额本就等于目标值时返回 {@code null}
     */
    String saveBalanceAdjustment(@NotNull Long accountId, @NotNull BigDecimal targetBalance);

}
