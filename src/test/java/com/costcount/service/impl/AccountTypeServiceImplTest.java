package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.costcount.dto.account.type.AccountTypeQueryDTO;
import com.costcount.dto.account.type.AccountTypeSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.AccountProvider;
import com.costcount.entity.AccountType;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountProviderMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.vo.account.type.AccountTypeVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AccountTypeServiceImplTest {

    @Mock
    private AccountTypeMapper accountTypeMapper;

    @Mock
    private AccountProviderMapper accountProviderMapper;

    @Mock
    private AccountMapper accountMapper;

    private AccountTypeServiceImpl service;

    @BeforeAll
    static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, Account.class);
        TableInfoHelper.initTableInfo(assistant, AccountProvider.class);
        TableInfoHelper.initTableInfo(assistant, AccountType.class);
    }

    @BeforeEach
    void setUp() {
        service = new AccountTypeServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", accountTypeMapper);
        ReflectionTestUtils.setField(service, "accountProviderMapper", accountProviderMapper);
        ReflectionTestUtils.setField(service, "accountMapper", accountMapper);
    }

    @Test
    void addAccountTypeShouldNormalizeInputAndReturnId() {
        when(accountProviderMapper.selectById(10001L)).thenReturn(new AccountProvider());
        when(accountTypeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(accountTypeMapper.insert(any(AccountType.class))).thenAnswer(invocation -> {
            AccountType accountType = invocation.getArgument(0);
            accountType.setId(20001L);
            return 1;
        });

        AccountTypeSaveDTO dto = new AccountTypeSaveDTO();
        dto.setProviderId(10001L);
        dto.setTypeCode("  DEBIT  ");
        dto.setTypeName("  储蓄卡  ");

        assertEquals("20001", service.addAccountType(dto));

        ArgumentCaptor<AccountType> captor = ArgumentCaptor.forClass(AccountType.class);
        verify(accountTypeMapper).insert(captor.capture());
        assertEquals("DEBIT", captor.getValue().getTypeCode());
        assertEquals("储蓄卡", captor.getValue().getTypeName());
        assertEquals(0, captor.getValue().getSort());
    }

    @Test
    void addAccountTypeShouldRejectMissingProvider() {
        when(accountProviderMapper.selectById(10001L)).thenReturn(null);

        AccountTypeSaveDTO dto = new AccountTypeSaveDTO();
        dto.setProviderId(10001L);
        dto.setTypeCode("DEBIT");
        dto.setTypeName("储蓄卡");

        BizException exception = assertThrows(BizException.class, () -> service.addAccountType(dto));

        assertEquals(404, exception.getCode());
    }

    @Test
    void listAccountTypesShouldReturnProviderName() {
        AccountTypeVO accountType = new AccountTypeVO();
        accountType.setId(20001L);
        accountType.setProviderId(10001L);
        accountType.setProviderName("中国银行");
        accountType.setTypeCode("DEBIT");
        accountType.setTypeName("储蓄卡");
        when(accountTypeMapper.selectJoinList(any(Class.class), any(MPJLambdaWrapper.class))).thenReturn(List.of(accountType));

        AccountTypeQueryDTO query = new AccountTypeQueryDTO();
        query.setTypeName(" 储蓄 ");

        List<AccountTypeVO> result = service.listAccountTypes(query);

        assertEquals(1, result.size());
        assertEquals("中国银行", result.getFirst().getProviderName());
        assertEquals(20001L, result.getFirst().getId());
    }

    @Test
    void updateAccountTypeShouldReadExistingEntityAndReturnId() {
        AccountType accountType = new AccountType();
        accountType.setId(20001L);
        when(accountTypeMapper.selectById(20001L)).thenReturn(accountType);
        when(accountProviderMapper.selectById(10001L)).thenReturn(new AccountProvider());
        when(accountTypeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(accountTypeMapper.updateById(any(AccountType.class))).thenReturn(1);

        AccountTypeSaveDTO dto = new AccountTypeSaveDTO();
        dto.setId(20001L);
        dto.setProviderId(10001L);
        dto.setTypeCode("CREDIT");
        dto.setTypeName("信用卡");

        assertEquals("20001", service.updateAccountType(dto));
        verify(accountTypeMapper).updateById(accountType);
        assertEquals(20001L, accountType.getId());
    }

    @Test
    void deleteAccountTypesShouldRejectTypesReferencedByAccounts() {
        when(accountMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BizException exception = assertThrows(
                BizException.class,
                () -> service.deleteAccountTypes(List.of(20001L))
        );

        assertEquals(409, exception.getCode());
    }
}
