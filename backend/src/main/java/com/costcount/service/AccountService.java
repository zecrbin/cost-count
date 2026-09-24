package com.costcount.service;

import com.costcount.dto.account.AccountInitialBalanceAdjustDTO;
import com.costcount.dto.account.AccountQueryDTO;
import com.costcount.dto.account.AccountSaveDTO;
import com.costcount.entity.Account;
import com.costcount.vo.account.AccountVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

/**
 * 账户服务。
 *
 * <p>账户余额只由流水回放维护，账户资料接口不能直接修改余额。</p>
 */
public interface AccountService extends MPJBaseService<Account> {

    /** 查询账户列表，带出账户类型、提供方和初始资金。 */
    List<AccountVO> listAccounts(AccountQueryDTO query);

    /** 查询账户详情。 */
    AccountVO getAccount(Long id);

    /** 新增账户，同时写入初始资金流水；填写尾号且未指定图标时生成银行卡图标。 */
    String addAccount(AccountSaveDTO dto);

    /** 修改账户资料；初始资金请使用 {@link #adjustInitialBalance}。 */
    String updateAccount(AccountSaveDTO dto);

    /** 修正账户初始资金并重算余额。 */
    void adjustInitialBalance(Long id, AccountInitialBalanceAdjustDTO dto);

    /** 批量删除只有初始资金流水的账户。 */
    void deleteAccounts(List<Long> accountIds);
}
