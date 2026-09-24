package com.costcount.mapper;

import com.costcount.entity.Transaction;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 流水数据访问接口，使用 MyBatis-Plus 通用 CRUD。 */
@Mapper
public interface TransactionMapper extends MPJBaseMapper<Transaction> {
}
