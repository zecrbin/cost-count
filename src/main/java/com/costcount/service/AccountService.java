package com.costcount.service;

import com.costcount.dto.AccountQueryDTO;
import com.costcount.dto.AccountSaveDTO;
import com.costcount.entity.Account;
import com.costcount.vo.AccountVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

/**
 * 账户查询及维护服务。
 */
public interface AccountService extends MPJBaseService<Account> {

    /** 根据提供方、账户类型、状态或关键字查询账户。 */
    List<AccountVO> listAllAccount(AccountQueryDTO dto);

    /** 查询账户详情；账户不存在时返回 {@code null}。 */
    AccountVO getAccount(Long id);

    /** 创建账户，并按账户名称生成需要的动态图标。 */
    String createAccount(AccountSaveDTO dto);

    /** 修改账户资料；余额只能通过流水变化。 */
    String updateAccount(AccountSaveDTO dto);

    /** 删除账户及其关联的动态图标文件。 */
    void deleteAccount(Long id);
}
