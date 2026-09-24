package com.costcount.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.costcount.common.PageQuery;
import com.costcount.dto.transaction.InitialTransactionSaveDTO;
import com.costcount.dto.transaction.TransactionQueryDTO;
import com.costcount.dto.transaction.TransactionSaveDTO;
import com.costcount.entity.*;
import com.costcount.mapper.*;
import com.costcount.service.TransactionService;
import com.costcount.vo.transaction.TransactionVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import static com.costcount.common.TransactionConstant.INITIAL;
import static com.costcount.common.TransactionConstant.SOURCE_SYSTEM;

/**
 * 流水记账服务。
 *
 * <p>流水是账户余额的事实来源；新增或修改流水后，通过顺序回放相关流水同步账户余额和日余额缓存。</p>
 */
@Service
public class TransactionServiceImpl
        extends MPJBaseServiceImpl<TransactionMapper, Transaction>
        implements TransactionService {

    @Resource
    private TransactionMapper transactionMapper;

    @Override
    public Page<TransactionVO> pageQueryTransactions(PageQuery<TransactionQueryDTO> pageQuery) {

        TransactionQueryDTO params = Optional.ofNullable(pageQuery.getParams()).orElseGet(TransactionQueryDTO::new);

        MPJLambdaWrapper<Transaction> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(
                Transaction::getId,
                Transaction::getTransactionType,
                Transaction::getCategoryId,
                Transaction::getAccountId,
                Transaction::getTargetAccountId,
                Transaction::getAmount,
                Transaction::getBalanceChange,
                Transaction::getBalanceAfter,
                Transaction::getTransactionTime,
                Transaction::getCounterparty,
                Transaction::getSource,
                Transaction::getRequestId,
                Transaction::getRemark
                )
                .selectAs("account", Account::getAccName, TransactionVO::getAccountName)
                .selectAs("targetAccount", Account::getAccName, TransactionVO::getTargetAccountName)
                .leftJoin(Account.class, "account", Account::getId, Transaction::getAccountId)
                .leftJoin(Account.class, "targetAccount", Account::getId, Transaction::getTargetAccountId)
                .and(w -> w
                        .eq(params.getAccountId() != null, Transaction::getAccountId, params.getAccountId())
                        .or()
                        .eq(params.getAccountId() != null, Transaction::getTargetAccountId, params.getAccountId()
                ))
                .eq(StringUtils.isNotBlank(params.getTransactionType()), Transaction::getTransactionType,
                        params.getTransactionType())
                .ge(params.getStartTime() != null, Transaction::getTransactionTime, params.getStartTime())
                .le(params.getEndTime() != null, Transaction::getTransactionTime, params.getEndTime())
                .orderByDesc(Transaction::getTransactionTime)
                .orderByDesc(Transaction::getId);

        Page<TransactionVO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        page = transactionMapper.selectJoinPage(page, TransactionVO.class, wrapper);

        return page;
    }

    @Override
    public String saveInitialTransaction(InitialTransactionSaveDTO initialTransactionSaveDTO) {

        Transaction transaction = new Transaction();
        transaction.setTransactionType(INITIAL);
        transaction.setAccountId(initialTransactionSaveDTO.getAccountId());
        transaction.setAmount(initialTransactionSaveDTO.getAmount());
        transaction.setTransactionTime(initialTransactionSaveDTO.getTransactionTime());
        transaction.setCategoryId(null);
        transaction.setBalanceChange(initialTransactionSaveDTO.getAmount());
        transaction.setBalanceAfter(initialTransactionSaveDTO.getAmount());
        transaction.setCounterparty(null);
        transaction.setSource(SOURCE_SYSTEM);
        transaction.setRequestId(null);

        save(transaction);

        return String.valueOf(transaction.getId());
    }

    @Override
    public String saveTransaction(TransactionSaveDTO transactionSaveDTO) {



        return "";
    }
}
