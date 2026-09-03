package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.costcount.dto.account.provider.AccountProviderQueryDTO;
import com.costcount.dto.account.provider.AccountProviderSaveDTO;
import com.costcount.entity.AccountProvider;
import com.costcount.entity.AccountType;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountProviderMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.vo.account.provider.AccountProviderVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AccountProviderServiceImplTest {

    @Mock
    private AccountProviderMapper accountProviderMapper;

    @Mock
    private AccountTypeMapper accountTypeMapper;

    private AccountProviderServiceImpl service;

    @BeforeAll
    static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, AccountProvider.class);
        TableInfoHelper.initTableInfo(assistant, AccountType.class);
    }

    @BeforeEach
    void setUp() {
        service = new AccountProviderServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", accountProviderMapper);
        ReflectionTestUtils.setField(service, "accountTypeMapper", accountTypeMapper);
    }

    @Test
    void addAccountProviderShouldNormalizeInputAndReturnId() {
        when(accountProviderMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(accountProviderMapper.insert(any(AccountProvider.class))).thenAnswer(invocation -> {
            AccountProvider provider = invocation.getArgument(0);
            provider.setId(10001L);
            return 1;
        });

        AccountProviderSaveDTO dto = new AccountProviderSaveDTO();
        dto.setProviderName("  招商银行  ");
        dto.setIcon("   ");

        assertEquals("10001", service.addAccountProvider(dto));

        ArgumentCaptor<AccountProvider> captor = ArgumentCaptor.forClass(AccountProvider.class);
        verify(accountProviderMapper).insert(captor.capture());
        assertEquals("招商银行", captor.getValue().getProviderName());
        assertNull(captor.getValue().getIcon());
    }

    @Test
    void addAccountProviderShouldRejectDuplicateName() {
        when(accountProviderMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        AccountProviderSaveDTO dto = new AccountProviderSaveDTO();
        dto.setProviderName("招商银行");

        BizException exception = assertThrows(
                BizException.class,
                () -> service.addAccountProvider(dto)
        );

        assertEquals(409, exception.getCode());
    }

    @Test
    void listAccountProvidersShouldReturnStronglyTypedViewObjects() {
        AccountProvider provider = new AccountProvider();
        provider.setId(10001L);
        provider.setProviderName("招商银行");
        provider.setIcon("providers/cmb.png");
        when(accountProviderMapper.selectJoinList(any(Class.class), any(MPJLambdaWrapper.class)))
                .thenReturn(List.of(toVO(provider)));

        AccountProviderQueryDTO query = new AccountProviderQueryDTO();
        query.setProviderName(" 招商 ");

        List<AccountProviderVO> result = service.listAccountProviders(query);

        assertEquals(1, result.size());
        assertEquals(10001L, result.getFirst().getId());
        assertEquals("招商银行", result.getFirst().getProviderName());
    }

    @Test
    void updateAccountProviderShouldReadIdFromDto() {
        AccountProvider provider = new AccountProvider();
        provider.setId(10001L);
        provider.setProviderName("招商银行");
        when(accountProviderMapper.selectById(10001L)).thenReturn(provider);
        when(accountProviderMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(accountProviderMapper.updateById(any(AccountProvider.class))).thenReturn(1);

        AccountProviderSaveDTO dto = new AccountProviderSaveDTO();
        dto.setId(10001L);
        dto.setProviderName("招商银行股份有限公司");

        assertEquals("10001", service.updateAccountProvider(dto));
        verify(accountProviderMapper).selectById(10001L);
    }

    @Test
    void deleteAccountProvidersShouldRejectProvidersReferencedByAccountTypes() {
        when(accountTypeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BizException exception = assertThrows(
                BizException.class,
                () -> service.deleteAccountProviders(List.of(10001L))
        );

        assertEquals(409, exception.getCode());
    }

    private AccountProviderVO toVO(AccountProvider provider) {
        AccountProviderVO vo = new AccountProviderVO();
        vo.setId(provider.getId());
        vo.setProviderName(provider.getProviderName());
        vo.setIcon(provider.getIcon());
        return vo;
    }
}
