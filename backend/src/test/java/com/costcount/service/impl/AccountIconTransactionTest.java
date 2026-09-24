package com.costcount.service.impl;

import com.costcount.entity.Account;
import com.costcount.mapper.AccountMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountIconTransactionTest {

    @TempDir
    Path iconDirectory;

    @Test
    void rollbackShouldKeepOldIconAndRemoveNewIcon() {
        verifyIconChange(false);
    }

    @Test
    void commitShouldRemoveOldIconAndKeepNewIcon() {
        verifyIconChange(true);
    }

    @Test
    void accountDetailsUpdateShouldNotWriteBalance() {
        AccountServiceImpl service = new AccountServiceImpl();
        AccountMapper mapper = mock(AccountMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("100.00"));
        account.setAccName("Updated");

        ReflectionTestUtils.invokeMethod(service, "updateAccountDetails", account);

        ArgumentCaptor<Account> update = ArgumentCaptor.forClass(Account.class);
        verify(mapper).updateById(update.capture());
        assertEquals("Updated", update.getValue().getAccName());
        assertNull(update.getValue().getBalance());
    }

    private void verifyIconChange(boolean commit) {
        AccountIconStorageServiceImpl storage = new AccountIconStorageServiceImpl();
        ReflectionTestUtils.setField(storage, "accountIconDir", iconDirectory.toString());
        String oldIcon = storage.storeAccountIcon(1L, null, "Bank", "1234");
        Account account = new Account();
        account.setId(1L);
        account.setAccTailNum("5678");
        account.setIcon(oldIcon);

        AccountServiceImpl service = new AccountServiceImpl();
        ReflectionTestUtils.setField(service, "accountIconStorageService", storage);
        AccountServiceImpl.AccountTypeInfoVO typeInfo = new AccountServiceImpl.AccountTypeInfoVO();
        typeInfo.setProviderName("Bank");

        TransactionSynchronizationManager.initSynchronization();
        try {
            ReflectionTestUtils.invokeMethod(service, "refreshIcon", account, typeInfo);
            String newIcon = account.getIcon();
            assertNotEquals(oldIcon, newIcon);
            assertTrue(Files.exists(iconPath(oldIcon)));
            assertTrue(Files.exists(iconPath(newIcon)));

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            if (commit) {
                synchronizations.forEach(TransactionSynchronization::afterCommit);
            }
            synchronizations.forEach(sync -> sync.afterCompletion(commit
                    ? TransactionSynchronization.STATUS_COMMITTED
                    : TransactionSynchronization.STATUS_ROLLED_BACK));

            assertTrue(Files.exists(iconPath(commit ? newIcon : oldIcon)));
            assertFalse(Files.exists(iconPath(commit ? oldIcon : newIcon)));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Path iconPath(String icon) {
        return iconDirectory.resolve(icon.substring("accounts/".length()));
    }
}
