package com.costcount.mapper;

import com.costcount.entity.AccountDailyBalance;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 每日余额汇总数据访问接口。 */
@Mapper
public interface AccountDailyBalanceMapper extends MPJBaseMapper<AccountDailyBalance> {
}
