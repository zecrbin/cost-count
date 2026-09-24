package com.costcount.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.costcount.entity.Transaction;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 流水数据访问接口，使用 MyBatis-Plus 通用 CRUD。 */
@Mapper
public interface TransactionMapper extends MPJBaseMapper<Transaction> {

    /**
     * 按 (transaction_time, id) 升序取账户自指定日期起的全部流水，含它作为转入方的记录。
     *
     * <p>重放流水和重建日快照都依赖同一份顺序，放在这里共用，避免两处各写一遍条件而漂移。</p>
     *
     * @param fromDate 起始日期（含）；为空时取全部
     */
    default List<Transaction> selectAccountTransactionsFrom(Long accountId, LocalDate fromDate) {
        LambdaQueryWrapper<Transaction> wrapper = new LambdaQueryWrapper<Transaction>()
                .and(w -> w.eq(Transaction::getAccountId, accountId)
                        .or().eq(Transaction::getTargetAccountId, accountId))
                .orderByAsc(Transaction::getTransactionTime)
                .orderByAsc(Transaction::getId);
        wrapper.ge(fromDate != null, Transaction::getTransactionTime,
                fromDate == null ? null : fromDate.atStartOfDay());
        return selectList(wrapper);
    }

    /** 查询账户最近一笔流水的发生时间，含它作为转入方的记录；没有流水时返回 null。 */
    default LocalDateTime selectLatestTransactionTime(Long accountId) {
        Transaction latest = selectOne(new LambdaQueryWrapper<Transaction>()
                .select(Transaction::getTransactionTime)
                .and(w -> w.eq(Transaction::getAccountId, accountId)
                        .or().eq(Transaction::getTargetAccountId, accountId))
                .isNotNull(Transaction::getTransactionTime)
                .orderByDesc(Transaction::getTransactionTime)
                .last("LIMIT 1"));
        return latest == null ? null : latest.getTransactionTime();
    }
}
