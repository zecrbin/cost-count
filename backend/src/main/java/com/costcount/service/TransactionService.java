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

/**
 * 交易流水服务。
 */
public interface TransactionService extends MPJBaseService<Transaction> {

    Page<TransactionVO> pageQueryTransactions(PageQuery<TransactionQueryDTO> pageQuery);

    String saveInitialTransaction(@Valid InitialTransactionSaveDTO initialTransactionSaveDTO);

    String saveTransaction(@Valid TransactionSaveDTO transactionSaveDTO);


}