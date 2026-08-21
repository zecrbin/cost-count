package com.costcount.service.impl;

import com.costcount.dto.AccountSaveDTO;
import com.costcount.entity.Account;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountMapper;
import com.costcount.service.AccountService;
import com.costcount.vo.AccountVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl extends MPJBaseServiceImpl<AccountMapper, Account> implements AccountService {
    @Resource
    private AccountMapper accountMapper;

    @Override
    public List<AccountVO> listAll() {
        List<Account> accounts = lambdaQuery().orderByAsc(Account::getSort).orderByAsc(Account::getId).list();
        return Optional.ofNullable(accounts).orElseGet(List::of).stream().map(account -> {
            AccountVO vo = new AccountVO();
            BeanUtils.copyProperties(account, vo);
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(AccountSaveDTO dto) {
        Account account = new Account();
        BeanUtils.copyProperties(dto, account);
        account.setInitialBalance(dto.getBalance());
        account.setColor(dto.getColor() == null ? "#3154E5" : dto.getColor());
        account.setSort(Math.toIntExact(count() + 1));
        accountMapper.insert(account);
        return account.getId().toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String update(Long id, AccountSaveDTO dto) {
        Account account = getById(id);
        if (account == null) {
            throw new BizException(404, "账户不存在");
        }
        BeanUtils.copyProperties(dto, account, "initialBalance");
        updateById(account);
        return id.toString();
    }
}
