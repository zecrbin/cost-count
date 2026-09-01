package com.costcount.service;

import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.Transaction;
import com.github.yulichang.base.MPJBaseService;

/**
 * 交易流水服务。
 *
 * <p>所有余额变更都必须通过本服务完成，避免直接修改账户余额造成账实不符。</p>
 */
public interface TransactionService extends MPJBaseService<Transaction> {

    /** 创建普通流水，并同步更新主账户及可选目标账户。 */
    String createTransaction(TransactionSaveDTO dto);

    /** 创建账户时写入唯一的初始余额流水。 */
    void createInitialTransaction(Long accountId, java.math.BigDecimal amount, String requestId);

    /** 新增反向流水冲正原流水，并重建受影响账户的余额快照。 */
    String reverseTransaction(Long id);

    /** 判断账户是否已经产生过流水，用于阻止删除有账务历史的账户。 */
    boolean existsForAccount(Long accountId);
}
