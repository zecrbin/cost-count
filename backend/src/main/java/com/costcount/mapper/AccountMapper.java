package com.costcount.mapper;

import com.costcount.entity.Account;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AccountMapper
    extends MPJBaseMapper<Account> {

    /** 按主键升序对账户加行锁，余额联动前先锁定，防止并发丢失更新和转账双账户死锁。 */
    @Select("""
            <script>
            SELECT *
            FROM cc_account
            WHERE is_deleted = 0
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY id
            FOR UPDATE
            </script>
            """)
    List<Account> selectByIdsForUpdate(@Param("ids") Collection<Long> ids);
}
