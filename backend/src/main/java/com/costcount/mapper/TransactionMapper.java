package com.costcount.mapper;

import com.costcount.entity.Transaction;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 流水数据访问接口，使用 MyBatis-Plus 通用 CRUD。 */
@Mapper
public interface TransactionMapper extends MPJBaseMapper<Transaction> {

    /**
     * 按幂等号查询流水，包含已逻辑删除的记录。
     *
     * <p>{@code uk_request_id} 唯一索引覆盖已删除行，幂等判断必须绕过逻辑删除过滤，否则重试会触发唯一键冲突。</p>
     */
    @Select("SELECT id, is_deleted FROM cc_transaction WHERE request_id = #{requestId} LIMIT 1")
    Transaction selectByRequestIdIncludingDeleted(String requestId);
}
