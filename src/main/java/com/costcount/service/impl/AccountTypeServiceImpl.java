package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.costcount.dto.account.type.AccountTypeQueryDTO;
import com.costcount.dto.account.type.AccountTypeSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.AccountProvider;
import com.costcount.entity.AccountType;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountProviderMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.service.AccountTypeService;
import com.costcount.vo.account.type.AccountTypeVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

import static com.costcount.common.CommonConstant.DEFAULT_SORT;

@Service
public class AccountTypeServiceImpl
        extends MPJBaseServiceImpl<AccountTypeMapper, AccountType>
        implements AccountTypeService {

    @Resource
    private AccountProviderMapper accountProviderMapper;

    @Resource
    private AccountMapper accountMapper;

    @Override
    public List<AccountTypeVO> listAccountTypes(AccountTypeQueryDTO query) {
        AccountTypeQueryDTO param = query == null ? new AccountTypeQueryDTO() : query;

        MPJLambdaWrapper<AccountType> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(
                        AccountType::getId,
                        AccountType::getProviderId,
                        AccountType::getTypeCode,
                        AccountType::getTypeName,
                        AccountType::getSort
                )
                .select(AccountProvider::getProviderName)
                .leftJoin(AccountProvider.class, AccountProvider::getId, AccountType::getProviderId)
                .eq(param.getProviderId() != null, AccountType::getProviderId, param.getProviderId())
                .like(StringUtils.hasText(param.getTypeCode()), AccountType::getTypeCode, normalize(param.getTypeCode()))
                .like(StringUtils.hasText(param.getTypeName()), AccountType::getTypeName, normalize(param.getTypeName()))
                .orderByAsc(AccountType::getSort)
                .orderByDesc(AccountType::getCreatedTime);

        return selectJoinList(AccountTypeVO.class, wrapper);
    }

    @Override
    public AccountTypeVO getAccountType(Long id) {
        MPJLambdaWrapper<AccountType> wrapper = new MPJLambdaWrapper<>();

        wrapper.select(
                        AccountType::getId,
                        AccountType::getProviderId,
                        AccountType::getTypeCode,
                        AccountType::getTypeName,
                        AccountType::getSort
                )
                .select(AccountProvider::getProviderName)
                .leftJoin(AccountProvider.class, AccountProvider::getId, AccountType::getProviderId)
                .eq(AccountType::getId, id);

        return selectJoinOne(AccountTypeVO.class, wrapper);
    }

    @Override
    public String addAccountType(AccountTypeSaveDTO dto) {
        validateSaveDTO(dto);

        AccountType accountType = new AccountType();
        accountType.setProviderId(dto.getProviderId());
        accountType.setTypeCode(normalize(dto.getTypeCode()));
        accountType.setTypeName(normalize(dto.getTypeName()));
        accountType.setSort(dto.getSort() == null ? DEFAULT_SORT : dto.getSort());
        accountType.setId(null);

        if (accountProviderMapper.selectById(accountType.getProviderId()) == null) {
            throw new BizException(404, "账户提供方不存在");
        }

        if (lambdaQuery().eq(AccountType::getProviderId, accountType.getProviderId())
                .eq(AccountType::getTypeCode, accountType.getTypeCode())
                .exists()) {
            throw new BizException(409, "该账户提供方下已存在相同编码的账户类型");
        }

        if (lambdaQuery().eq(AccountType::getProviderId, accountType.getProviderId())
                .eq(AccountType::getTypeName, accountType.getTypeName())
                .exists()) {
            throw new BizException(409, "该账户提供方下已存在相同名称的账户类型");
        }

        save(accountType);

        return String.valueOf(accountType.getId());
    }

    @Override
    public String updateAccountType(AccountTypeSaveDTO dto) {
        validateSaveDTO(dto);
        if (dto.getId() == null) {
            throw new BizException(400, "账户类型 ID 不能为空");
        }

        AccountType accountType = getById(dto.getId());
        if (accountType == null) {
            throw new BizException(404, "账户类型不存在");
        }

        accountType.setProviderId(dto.getProviderId());
        accountType.setTypeCode(normalize(dto.getTypeCode()));
        accountType.setTypeName(normalize(dto.getTypeName()));
        accountType.setSort(dto.getSort() == null ? DEFAULT_SORT : dto.getSort());

        if (accountProviderMapper.selectById(accountType.getProviderId()) == null) {
            throw new BizException(404, "账户提供方不存在");
        }

        if (lambdaQuery().eq(AccountType::getProviderId, accountType.getProviderId())
                .eq(AccountType::getTypeCode, accountType.getTypeCode())
                .ne(AccountType::getId, accountType.getId())
                .exists()) {
            throw new BizException(409, "该账户提供方下已存在相同编码的账户编码");
        }

        if (lambdaQuery().eq(AccountType::getProviderId, accountType.getProviderId())
                .eq(AccountType::getTypeName, accountType.getTypeName())
                .ne(AccountType::getId, accountType.getId())
                .exists()) {
            throw new BizException(409, "该账户提供方下已存在相同名称的账户名称");
        }

        updateById(accountType);
        return String.valueOf(dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountTypes(List<Long> accountTypeIds) {
        if (accountTypeIds == null || accountTypeIds.isEmpty()
                || accountTypeIds.stream().anyMatch(Objects::isNull)) {
            throw new BizException(400, "账户类型 ID 不能为空");
        }
        List<Long> ids = accountTypeIds.stream().distinct().toList();

        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                .in(Account::getTypeId, ids);
        if (accountMapper.selectCount(wrapper) > 0) {
            throw new BizException(409, "账户类型已被账户使用，无法删除");
        }

        removeByIds(ids);
    }

    private void validateSaveDTO(AccountTypeSaveDTO dto) {
        if (dto == null) {
            throw new BizException(400, "账户类型参数不能为空");
        }
        if (dto.getProviderId() == null) {
            throw new BizException(400, "账户提供方 ID 不能为空");
        }
        if (!StringUtils.hasText(dto.getTypeCode())) {
            throw new BizException(400, "账户类型编码不能为空");
        }
        if (dto.getTypeCode().trim().length() > 64) {
            throw new BizException(400, "账户类型编码不能超过64个字符");
        }
        if (!StringUtils.hasText(dto.getTypeName())) {
            throw new BizException(400, "账户类型名称不能为空");
        }
        if (dto.getTypeName().trim().length() > 128) {
            throw new BizException(400, "账户类型名称不能超过128个字符");
        }
        if (dto.getSort() != null && dto.getSort() < 0) {
            throw new BizException(400, "排序值不能小于0");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
