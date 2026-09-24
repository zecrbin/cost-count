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

import java.math.BigDecimal;

/**
 * 交易流水服务。
 *
 * <p>流水是账户余额的事实来源：每次新增、修改或删除流水后，从受影响日期起按时间顺序回放相关账户的流水，
 * 同步流水的 {@code balanceAfter}、账户当前余额和活动日余额快照。</p>
 */
public interface TransactionService extends MPJBaseService<Transaction> {

    Page<TransactionVO> pageQueryTransactions(PageQuery<TransactionQueryDTO> pageQuery);

    /** 查询流水详情。 */
    TransactionVO getTransaction(Long id);

    /** 为新账户写入初始资金流水并计算余额；每个账户只能有一条。 */
    String saveInitialTransaction(@Valid InitialTransactionSaveDTO initialTransactionSaveDTO);

    /** 修正账户初始资金，并重算该账户此后的全部余额。 */
    void updateInitialBalance(Long accountId, BigDecimal initialBalance, String remark);

    /** 删除账户的初始资金流水和日余额；账户存在其他流水时拒绝删除。 */
    void deleteAccountLedger(Long accountId);

    /** 新增收入、支出、转账或余额调整流水；携带已存在的幂等号时返回已有流水 ID。 */
    String saveTransaction(@Valid TransactionSaveDTO transactionSaveDTO);

    /** 修改流水并重算受影响账户的余额。 */
    String updateTransaction(@Valid TransactionSaveDTO transactionSaveDTO);

    /** 删除流水并重算受影响账户的余额。 */
    void deleteTransaction(Long id);
}
