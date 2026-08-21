package com.costcount;

import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Category;
import com.costcount.service.AccountService;
import com.costcount.service.CategoryService;
import com.costcount.service.TransactionRecordService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CostCountApplicationTests {
    @Resource
    private AccountService accountService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private TransactionRecordService transactionRecordService;

    @Test
    void shouldInitializeCommonCategories() {
        assertThat(categoryService.count()).isGreaterThanOrEqualTo(12);
    }

    @Test
    void shouldAdjustAndRestoreBalanceWithExpense() {
        List<Account> accounts = accountService.lambdaQuery().orderByAsc(Account::getId).list();
        Account account = accounts.getFirst();
        BigDecimal originalBalance = account.getBalance();
        TransactionSaveDTO dto = new TransactionSaveDTO();
        dto.setType("EXPENSE");
        dto.setAmount(new BigDecimal("12.34"));
        List<Category> categories = categoryService.lambdaQuery().eq(Category::getType, "EXPENSE").orderByAsc(Category::getId).list();
        dto.setCategoryId(categories.getFirst().getId());
        dto.setAccountId(account.getId());
        dto.setTransactionDate(LocalDate.now());
        dto.setMerchant("集成测试");
        String id = transactionRecordService.create(dto);

        assertThat(accountService.getById(account.getId()).getBalance()).isEqualByComparingTo(originalBalance.subtract(dto.getAmount()));
        transactionRecordService.delete(Long.valueOf(id));
        assertThat(accountService.getById(account.getId()).getBalance()).isEqualByComparingTo(originalBalance);
    }
}
