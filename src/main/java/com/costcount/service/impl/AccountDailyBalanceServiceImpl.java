package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.costcount.entity.AccountDailyBalance;
import com.costcount.entity.Transaction;
import com.costcount.common.CommonConstant;
import com.costcount.mapper.AccountDailyBalanceMapper;
import com.costcount.mapper.TransactionMapper;
import com.costcount.service.AccountDailyBalanceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/* 每日汇总实现；每次重建都从流水事实重新计算，避免缓存链条累积误差。 */
public class AccountDailyBalanceServiceImpl implements AccountDailyBalanceService {

    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private AccountDailyBalanceMapper dailyBalanceMapper;

    @Override
    @Transactional
    public void rebuildFrom(Long accountId, LocalDate fromDate) {
        // 只读取已入账且涉及该账户的流水；MyBatis-Plus 会自动过滤逻辑删除记录。
        List<Transaction> transactions = transactionMapper.selectList(Wrappers.lambdaQuery(Transaction.class)
                .and(wrapper -> wrapper.eq(Transaction::getAccountId, accountId)
                        .or().eq(Transaction::getTargetAccountId, accountId))
                .in(Transaction::getStatus,
                        CommonConstant.TRANSACTION_POSTED, CommonConstant.TRANSACTION_REVERSED)
                .orderByAsc(Transaction::getTransactionTime)
                .orderByAsc(Transaction::getId));

        Map<LocalDate, BigDecimal> changes = new HashMap<>();
        BigDecimal opening = BigDecimal.ZERO;
        LocalDate endDate = LocalDate.now();
        for (Transaction transaction : transactions) {
            LocalDate date = transaction.getTransactionTime().toLocalDate();
            BigDecimal change = transaction.getAccountId().equals(accountId)
                    ? transaction.getBalanceChange()
                    : transaction.getTargetBalanceChange();
            if (change == null) {
                continue;
            }
            if (date.isBefore(fromDate)) {
                opening = opening.add(change);
            } else {
                changes.merge(date, change, BigDecimal::add);
                if (date.isAfter(endDate)) {
                    endDate = date;
                }
            }
        }

        // 从发生日开始逐日落盘，补录历史流水时后续日期会自然得到新的期初余额。
        BigDecimal running = opening;
        for (LocalDate date = fromDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            BigDecimal transactionChange = changes.getOrDefault(date, BigDecimal.ZERO);
            // correction_change 预留给人工修正；纯流水重建时不额外制造修正量。
            BigDecimal correctionChange = BigDecimal.ZERO;
            BigDecimal closing = running.add(transactionChange).add(correctionChange);
            AccountDailyBalance daily = dailyBalanceMapper.selectOne(Wrappers.lambdaQuery(AccountDailyBalance.class)
                    .eq(AccountDailyBalance::getAccountId, accountId)
                    .eq(AccountDailyBalance::getStatDate, date));
            if (daily == null) {
                daily = new AccountDailyBalance();
                daily.setAccountId(accountId);
                daily.setStatDate(date);
            }
            daily.setOpeningBalance(running);
            daily.setTransactionChange(transactionChange);
            daily.setCorrectionChange(correctionChange);
            daily.setClosingBalance(closing);
            daily.setRebuiltTime(LocalDateTime.now());
            if (daily.getId() == null) {
                dailyBalanceMapper.insert(daily);
            } else {
                dailyBalanceMapper.updateById(daily);
            }
            running = closing;
        }
    }
}
