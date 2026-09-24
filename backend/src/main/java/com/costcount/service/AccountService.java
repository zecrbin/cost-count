package com.costcount.service;

import com.costcount.dto.account.AccountBalanceDto;
import com.costcount.dto.account.AccountQueryDTO;
import com.costcount.dto.account.AccountSaveDTO;
import com.costcount.entity.Account;
import com.costcount.vo.account.AccountVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.Collection;
import java.util.List;

/**
 * 账户服务。
 */
public interface AccountService extends MPJBaseService<Account> {

    List<AccountVO> listAccounts(AccountQueryDTO query);

    AccountVO getAccount(Long id);

    String saveAccount(AccountSaveDTO accountSaveDTO);

    /** 修改账户资料；初始资金和余额不在此修改，余额请走 {@link #updateBalance}。 */
    String updateAccount(AccountSaveDTO accountSaveDTO);

    /** 删除账户；账户只剩初始流水时才能删除，有其他流水的请改为停用。 */
    void deleteAccounts(List<Long> accountIds);

    String updateBalance(AccountBalanceDto accountBalanceDto);

    /**
     * 按当前的类型和提供方资料，重新生成这些账户类型下所有账户的名称和图标。
     *
     * <p>账户名和图标由提供方名、提供方图标、类型名派生，这些资料变更后需调用此方法同步。</p>
     */
    void refreshAccountsOfTypes(Collection<Long> typeIds);
}
