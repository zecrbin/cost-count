package com.costcount.mapper;

import com.costcount.entity.Transaction;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionMapper extends MPJBaseMapper<Transaction> {
}
