package com.costcount.common;

import java.math.BigDecimal;

import static com.costcount.common.TransactionConstant.EXPENSE;
import static com.costcount.common.TransactionConstant.INCOME;
import static com.costcount.common.TransactionConstant.INITIAL;
import static com.costcount.common.TransactionConstant.TRANSFER;

/**
 * 流水对账户余额的影响规则。
 *
 * <p>存储账户（DEBIT）的余额是可用资金，信用账户（CREDIT）的余额是待还金额，
 * 因此同一类流水在两种账户上的余额方向相反。</p>
 *
 * <table>
 *     <tr><th>流水</th><th>DEBIT</th><th>CREDIT</th></tr>
 *     <tr><td>INITIAL 初始资金</td><td>+金额</td><td>+金额（初始欠款）</td></tr>
 *     <tr><td>INCOME 收入</td><td>+金额</td><td>-金额（如退款冲抵欠款）</td></tr>
 *     <tr><td>EXPENSE 支出</td><td>-金额</td><td>+金额</td></tr>
 *     <tr><td>TRANSFER 转出</td><td>-金额</td><td>+金额（如信用卡取现）</td></tr>
 *     <tr><td>TRANSFER 转入</td><td>+金额</td><td>-金额（如还款）</td></tr>
 * </table>
 *
 * <p>ADJUSTMENT 余额调整直接使用调用方给出的带符号变化值，不经过本规则换算。</p>
 */
public final class LedgerBalance {

    private LedgerBalance() {
    }

    /** 计算流水主账户（转账时为转出账户）的余额变化。 */
    public static BigDecimal mainChange(String transactionType, BigDecimal amount, boolean credit) {
        return switch (transactionType) {
            case INITIAL -> amount;
            case INCOME -> credit ? amount.negate() : amount;
            case EXPENSE, TRANSFER -> credit ? amount : amount.negate();
            default -> throw new IllegalArgumentException("不支持按金额换算余额变化的流水类型：" + transactionType);
        };
    }

    /** 计算转账目标账户的余额变化。 */
    public static BigDecimal targetChange(BigDecimal amount, boolean credit) {
        return credit ? amount.negate() : amount;
    }
}
