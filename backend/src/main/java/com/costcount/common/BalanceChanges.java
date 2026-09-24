package com.costcount.common;

import com.costcount.entity.Transaction;

import java.math.BigDecimal;

import static com.costcount.common.TransactionConstant.TRANSFER;

/** 单笔流水对某个账户的余额影响规则，供流水重放和日余额重建共用。 */
public final class BalanceChanges {

    private BalanceChanges() {
    }

    /**
     * 计算指定账户在一笔流水中的余额变化。
     *
     * <p>主账户直接使用已落库的变化值；转账目标账户没有独立变化字段，需要按账户性质反推。
     * 资产账户转入增加余额，信用账户转入则减少待还金额。</p>
     */
    public static BigDecimal of(Long accountId, Transaction transaction, boolean credit) {
        if (accountId.equals(transaction.getAccountId())) {
            return transaction.getBalanceChange() == null ? BigDecimal.ZERO : transaction.getBalanceChange();
        }
        if (!TRANSFER.equals(transaction.getTransactionType()) || transaction.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return credit ? transaction.getAmount().negate() : transaction.getAmount();
    }
}
