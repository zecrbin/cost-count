package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.costcount.dto.transaction.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Transaction;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.TransactionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionLockOrderTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), Transaction.class);
    }

    @Test
    void idempotencyReadShouldFollowAccountLock() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionMapper transactionMapper = mock(TransactionMapper.class);
        TransactionServiceImpl service = new TransactionServiceImpl();
        ReflectionTestUtils.setField(service, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(service, "transactionMapper", transactionMapper);

        Account account = new Account();
        account.setId(1L);
        Transaction existing = new Transaction();
        existing.setId(2L);
        when(accountMapper.selectByIdsForUpdate(any())).thenReturn(List.of(account));
        when(transactionMapper.selectOne(org.mockito.ArgumentMatchers.<Wrapper<Transaction>>any()))
                .thenReturn(existing);

        TransactionSaveDTO dto = new TransactionSaveDTO();
        dto.setTransactionType("ADJUSTMENT");
        dto.setAccountId(1L);
        dto.setBalanceChange(BigDecimal.ONE);
        dto.setTransactionTime(LocalDateTime.now().minusMinutes(1));
        dto.setRequestId("retry-1");

        assertEquals("2", service.saveTransaction(dto));
        InOrder order = inOrder(accountMapper, transactionMapper);
        order.verify(accountMapper).selectByIdsForUpdate(any());
        order.verify(transactionMapper).selectOne(org.mockito.ArgumentMatchers.<Wrapper<Transaction>>any());
    }
}
