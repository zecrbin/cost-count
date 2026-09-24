package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.costcount.dto.transaction.TransactionQueryDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Transaction;
import com.costcount.exception.BizException;
import com.costcount.dto.transaction.TransactionSaveDTO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionServiceImplTest {

    private final TransactionServiceImpl service = new TransactionServiceImpl();

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Transaction.class);
        TableInfoHelper.initTableInfo(assistant, Account.class);
    }

    @Test
    void pageQueryWithoutAccountShouldNotRenderEmptyParentheses() {
        String sql = service.buildPageQueryWrapper(new TransactionQueryDTO()).getSqlSegment();

        assertFalse(sql.contains("()"), sql);
        assertFalse(sql.contains("( AND"), sql);
    }

    @Test
    void pageQueryWithAccountShouldMatchSourceOrTargetAccount() {
        TransactionQueryDTO params = new TransactionQueryDTO();
        params.setAccountId(30001L);
        params.setTransactionType("EXPENSE");

        String sql = service.buildPageQueryWrapper(params).getSqlSegment();

        assertTrue(sql.contains("account_id"), sql);
        assertTrue(sql.contains("target_account_id"), sql);
        assertTrue(sql.contains(" OR "), sql);
        assertTrue(sql.contains("transaction_type"), sql);
    }

    @Test
    void saveTransactionShouldRejectUntilImplemented() {
        BizException exception = assertThrows(BizException.class,
                () -> service.saveTransaction(new TransactionSaveDTO()));

        assertEquals(501, exception.getCode());
    }
}
