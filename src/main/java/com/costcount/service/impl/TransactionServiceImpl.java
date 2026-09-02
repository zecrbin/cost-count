package com.costcount.service.impl;

import com.costcount.entity.Transaction;
import com.costcount.mapper.TransactionMapper;
import com.costcount.service.TransactionService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TransactionServiceImpl
        extends MPJBaseServiceImpl<TransactionMapper, Transaction>
        implements TransactionService {
}
