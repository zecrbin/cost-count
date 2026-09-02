package com.costcount.service.impl;

import com.costcount.dto.account.provider.AccountProviderQueryDTO;
import com.costcount.dto.account.provider.AccountProviderSaveDTO;
import com.costcount.entity.AccountProvider;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountProviderMapper;
import com.costcount.service.AccountProviderService;
import com.costcount.vo.account.provider.AccountProviderVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class AccountProviderServiceImpl
        extends MPJBaseServiceImpl<AccountProviderMapper, AccountProvider>
        implements AccountProviderService {

    @Override
    public List<AccountProviderVO> listAccountProviders(AccountProviderQueryDTO query) {
        String providerName = query == null ? null : normalize(query.getProviderName());

        MPJLambdaWrapper<AccountProvider> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(
                        AccountProvider::getId,
                        AccountProvider::getProviderName,
                        AccountProvider::getIcon
                )
                .like(StringUtils.hasText(providerName), AccountProvider::getProviderName, providerName);

        return selectJoinList(AccountProviderVO.class, wrapper);
    }

    @Override
    public AccountProviderVO getAccountProvider(Long id) {
        MPJLambdaWrapper<AccountProvider> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(AccountProvider::getId, AccountProvider::getProviderName, AccountProvider::getIcon)
                .eq(AccountProvider::getId, id);
        return selectJoinOne(AccountProviderVO.class, wrapper);
    }

    @Override
    public String addAccountProvider(AccountProviderSaveDTO dto) {
        validateSaveDTO(dto);
        String providerName = normalize(dto.getProviderName());

        if (lambdaQuery()
                .eq(AccountProvider::getProviderName, providerName)
                .exists()) {
            throw new BizException(409, "账户提供方名称已存在");
        }

        AccountProvider provider = new AccountProvider();
        provider.setProviderName(providerName);
        provider.setIcon(normalize(dto.getIcon()));

        save(provider);

        return String.valueOf(provider.getId());
    }

    @Override
    public String updateAccountProvider(AccountProviderSaveDTO dto) {
        validateSaveDTO(dto);
        Long id = dto.getId();

        AccountProvider provider = getById(id);
        if (provider == null) {
            throw new BizException(404, "账户提供方不存在");
        }

        String providerName = normalize(dto.getProviderName());

        boolean exists = lambdaQuery().eq(AccountProvider::getProviderName, providerName)
                .ne(AccountProvider::getId, id)
                .exists();

        if (exists) {
            throw new BizException(409, "账户提供方名称已存在");
        }

        provider.setProviderName(providerName);
        provider.setIcon(normalize(dto.getIcon()));

        updateById(provider);

        return String.valueOf(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountProviders(List<Long> accountProviderIds) {
        if (accountProviderIds == null || accountProviderIds.isEmpty()
                || accountProviderIds.stream().anyMatch(Objects::isNull)) {
            throw new BizException(400, "账户提供方ID不能为空");
        }
        List<Long> ids = accountProviderIds.stream().distinct().toList();

        Long accountTypeCount = lambdaQuery().in(AccountProvider::getId, ids).count();

        if (accountTypeCount > 0) {
            throw new BizException(409, "账户提供方已关联账户类型，无法删除");
        }

        removeByIds(ids);
    }

    private void validateSaveDTO(AccountProviderSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getProviderName())) {
            throw new BizException(400, "账户提供方名称不能为空");
        }
        if (dto.getProviderName().trim().length() > 64) {
            throw new BizException(400, "账户提供方名称不能超过64个字符");
        }
        if (dto.getIcon() != null && dto.getIcon().trim().length() > 255) {
            throw new BizException(400, "图标路径不能超过255个字符");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
