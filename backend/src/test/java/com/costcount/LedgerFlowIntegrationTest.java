package com.costcount;

import com.costcount.dto.account.AccountDailyBalanceQueryDTO;
import com.costcount.dto.account.AccountInitialBalanceAdjustDTO;
import com.costcount.dto.account.AccountSaveDTO;
import com.costcount.dto.account.provider.AccountProviderSaveDTO;
import com.costcount.dto.account.type.AccountTypeSaveDTO;
import com.costcount.dto.category.CategoryQueryDTO;
import com.costcount.dto.category.CategorySaveDTO;
import com.costcount.dto.transaction.TransactionSaveDTO;
import com.costcount.entity.Transaction;
import com.costcount.exception.BizException;
import com.costcount.service.AccountDailyBalanceService;
import com.costcount.service.AccountProviderService;
import com.costcount.service.AccountService;
import com.costcount.service.AccountTypeService;
import com.costcount.service.CategoryService;
import com.costcount.service.TransactionService;
import com.costcount.vo.account.AccountDailyBalanceVO;
import com.costcount.vo.account.AccountVO;
import com.costcount.vo.category.CategoryVO;
import com.costcount.vo.transaction.TransactionVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 覆盖开户、记账、转账、补录、修改、删除和初始资金修正的余额联动。 */
@SpringBootTest(properties = "app.storage.account-icon-dir=target/test-account-icons")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LedgerFlowIntegrationTest {

    @Resource
    private AccountProviderService accountProviderService;
    @Resource
    private AccountTypeService accountTypeService;
    @Resource
    private AccountService accountService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private TransactionService transactionService;
    @Resource
    private AccountDailyBalanceService accountDailyBalanceService;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MockMvc mockMvc;

    private Long debitTypeId;
    private Long creditTypeId;
    private Long lunchCategoryId;
    private Long salaryCategoryId;

    @BeforeEach
    void setUp() {
        for (String table : List.of("cc_account_daily_balance", "cc_transaction", "cc_category", "cc_account",
                "cc_account_type", "cc_account_provider")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        AccountProviderSaveDTO provider = new AccountProviderSaveDTO();
        provider.setProviderName("招商银行");
        Long providerId = Long.valueOf(accountProviderService.addAccountProvider(provider));
        debitTypeId = addAccountType(providerId, "debit", "储蓄卡");
        creditTypeId = addAccountType(providerId, "CREDIT", "信用卡");

        Long foodId = addCategory(null, "EXPENSE", "餐饮");
        lunchCategoryId = addCategory(foodId, "EXPENSE", "午餐");
        salaryCategoryId = addCategory(null, "INCOME", "工资");
    }

    @Test
    void ledgerShouldKeepBalancesConsistentThroughWholeFlow() {
        Long debit = addAccount(debitTypeId, "工资卡", "1000.00", time(1, 9));
        Long credit = addAccount(creditTypeId, "信用卡", "200.00", time(1, 9));
        assertBalance(debit, "1000.00");
        assertBalance(credit, "200.00");

        saveTransaction(dto -> expense(dto, debit, "100.00", time(2, 18)));
        saveTransaction(dto -> income(dto, debit, "50.00", time(3, 10)));
        saveTransaction(dto -> expense(dto, credit, "300.00", time(2, 20)));
        String transferId = saveTransaction(dto -> transfer(dto, debit, credit, "400.00", time(4, 9)));
        assertBalance(debit, "550.00");
        assertBalance(credit, "100.00");

        // 补录更早的支出，之后流水的余额快照和日余额都要重算。
        String backdatedId = saveTransaction(dto -> expense(dto, debit, "20.00", time(2, 12)));
        assertBalance(debit, "530.00");
        assertBalanceAfter(backdatedId, "980.00");
        assertEquals(List.of("1000.00", "880.00", "930.00", "530.00"), closingBalances(debit));

        // 把补录流水改为收入：余额变化方向反转。
        transactionService.updateTransaction(build(dto -> {
            income(dto, debit, "20.00", time(2, 12));
            dto.setId(Long.valueOf(backdatedId));
        }));
        assertBalance(debit, "570.00");
        Transaction updated = transactionService.getById(Long.valueOf(backdatedId));
        assertEquals(salaryCategoryId, updated.getCategoryId());
        assertNull(updated.getTargetAccountId());

        // 删除还款转账：两个账户都恢复。
        transactionService.deleteTransaction(Long.valueOf(transferId));
        assertBalance(debit, "970.00");
        assertBalance(credit, "500.00");

        // 修正初始资金后整条链路重算。
        AccountInitialBalanceAdjustDTO adjust = new AccountInitialBalanceAdjustDTO();
        adjust.setInitialBalance(new BigDecimal("2000.00"));
        accountService.adjustInitialBalance(debit, adjust);
        assertBalance(debit, "1970.00");
        assertEquals(List.of("2000.00", "1920.00", "1970.00"), closingBalances(debit));
        assertEquals(0, new BigDecimal("2000.00").compareTo(accountService.getAccount(debit).getInitialBalance()));
    }

    @Test
    void creditAccountShouldTreatBalanceAsDebt() {
        Long debit = addAccount(debitTypeId, "工资卡", "1000.00", time(1, 9));
        Long credit = addAccount(creditTypeId, "信用卡", "0", time(1, 9));

        saveTransaction(dto -> expense(dto, credit, "300.00", time(2, 9)));
        saveTransaction(dto -> income(dto, credit, "50.00", time(2, 10)));
        saveTransaction(dto -> transfer(dto, credit, debit, "100.00", time(2, 11)));
        assertBalance(credit, "350.00");
        assertBalance(debit, "1100.00");

        saveTransaction(dto -> {
            dto.setTransactionType("ADJUSTMENT");
            dto.setAccountId(credit);
            dto.setBalanceChange(new BigDecimal("-0.50"));
            dto.setTransactionTime(time(3, 9));
        });
        assertBalance(credit, "349.50");
    }

    @Test
    void requestIdShouldMakeCreateIdempotent() {
        Long debit = addAccount(debitTypeId, "工资卡", "100.00", time(1, 9));
        String first = saveTransaction(dto -> {
            expense(dto, debit, "10.00", time(2, 9));
            dto.setRequestId("client-1");
        });
        String retry = saveTransaction(dto -> {
            expense(dto, debit, "10.00", time(2, 9));
            dto.setRequestId("client-1");
        });
        assertEquals(first, retry);
        assertBalance(debit, "90.00");

        // 唯一索引覆盖已删除流水：删除后复用幂等号应明确拒绝，而不是触发唯一键冲突。
        transactionService.deleteTransaction(Long.valueOf(first));
        assertBizError(409, () -> saveTransaction(dto -> {
            expense(dto, debit, "10.00", time(2, 9));
            dto.setRequestId("client-1");
        }));
        assertBalance(debit, "100.00");

        String second = saveTransaction(dto -> {
            expense(dto, debit, "10.00", time(2, 9));
            dto.setRequestId("client-2");
        });
        TransactionVO detail = transactionService.getTransaction(Long.valueOf(second));
        assertEquals("午餐", detail.getCategoryName());
        assertEquals("工资卡", detail.getAccountName());
        assertBizError(404, () -> transactionService.getTransaction(-1L));
    }

    @Test
    void invalidTransactionsShouldBeRejected() {
        Long debit = addAccount(debitTypeId, "工资卡", "100.00", time(2, 9));
        Long other = addAccount(debitTypeId, "零钱卡", "0", time(2, 9));

        assertBizError(400, () -> saveTransaction(dto -> {
            expense(dto, debit, "10.00", time(3, 9));
            dto.setCategoryId(salaryCategoryId);
        }));
        assertBizError(400, () -> saveTransaction(dto -> transfer(dto, debit, debit, "10.00", time(3, 9))));
        assertBizError(400, () -> saveTransaction(dto -> expense(dto, debit, "10.00", time(1, 9))));
        assertBizError(400, () -> saveTransaction(dto -> {
            expense(dto, debit, "10.00", time(3, 9));
            dto.setTransactionType("INITIAL");
        }));

        Transaction initial = transactionService.lambdaQuery().eq(Transaction::getAccountId, debit).one();
        assertBizError(400, () -> transactionService.deleteTransaction(initial.getId()));

        AccountSaveDTO disable = accountSaveDTO(debitTypeId, "零钱卡");
        disable.setId(other);
        disable.setStatus(0);
        accountService.updateAccount(disable);
        assertBizError(400, () -> saveTransaction(dto -> transfer(dto, debit, other, "10.00", time(3, 9))));
        assertBalance(debit, "100.00");
    }

    @Test
    void accountDeletionAndTypeChangeShouldProtectLedger() {
        Long used = addAccount(debitTypeId, "工资卡", "100.00", time(1, 9));
        saveTransaction(dto -> expense(dto, used, "10.00", time(2, 9)));
        assertBizError(409, () -> accountService.deleteAccounts(List.of(used)));

        AccountSaveDTO switchType = accountSaveDTO(creditTypeId, "工资卡");
        switchType.setId(used);
        assertBizError(409, () -> accountService.updateAccount(switchType));

        Long unused = addAccount(debitTypeId, "新卡", "5.00", time(1, 9));
        accountService.deleteAccounts(List.of(unused));
        assertEquals(0, transactionService.lambdaQuery().eq(Transaction::getAccountId, unused).count());
        assertBizError(404, () -> accountService.getAccount(unused));
    }

    @Test
    void accountWithTailNumberShouldGetGeneratedIcon() {
        AccountSaveDTO dto = accountSaveDTO(creditTypeId, "招行信用卡");
        dto.setAccTailNum("1234");
        dto.setCreditLimit(new BigDecimal("50000"));
        Long id = Long.valueOf(accountService.addAccount(dto));

        AccountVO account = accountService.getAccount(id);
        assertEquals("accounts/" + id + ".png", account.getIcon());
        assertEquals("招商银行", account.getProviderName());
        assertEquals("CREDIT", account.getTypeCode());
        assertTrue(Files.exists(Path.of("target/test-account-icons", id + ".png")));
    }

    @Test
    void categoriesShouldFormTwoLevelTreeAndProtectUsage() {
        List<CategoryVO> tree = categoryService.listCategoryTree(query("EXPENSE"));
        assertEquals(1, tree.size());
        assertEquals("午餐", tree.get(0).getChildren().get(0).getCategoryName());

        assertBizError(400, () -> addCategory(lunchCategoryId, "EXPENSE", "工作日午餐"));
        assertBizError(400, () -> addCategory(tree.get(0).getId(), "INCOME", "错误类型"));
        assertBizError(409, () -> addCategory(tree.get(0).getId(), "EXPENSE", "午餐"));
        assertBizError(409, () -> categoryService.deleteCategories(List.of(tree.get(0).getId())));

        Long debit = addAccount(debitTypeId, "工资卡", "100.00", time(1, 9));
        saveTransaction(dto -> expense(dto, debit, "10.00", time(2, 9)));
        assertBizError(409, () -> categoryService.deleteCategories(List.of(lunchCategoryId)));

        // 历史数据中父 ID 为 NULL 的分类也视为一级分类。
        jdbcTemplate.update("INSERT INTO cc_category (id, pid, category_type, category_name, sort) "
                + "VALUES (1, NULL, 'EXPENSE', '交通', 9)");
        assertEquals(List.of("餐饮", "交通"), categoryService.listCategoryTree(query("EXPENSE")).stream()
                .map(CategoryVO::getCategoryName).toList());
        assertBizError(409, () -> addCategory(null, "EXPENSE", "交通"));
    }

    @Test
    void httpApiShouldAcceptDocumentedDateTimeFormat() throws Exception {
        Long debit = addAccount(debitTypeId, "工资卡", "100.00", time(1, 9));
        String body = """
                {"transactionType":"EXPENSE","accountId":"%s","categoryId":"%s","amount":12.5,
                 "transactionTime":"2026-09-05 10:00:00","counterparty":"便利店"}
                """.formatted(debit, lunchCategoryId);
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/transactions/page").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"params\":{\"accountId\":\"%s\",\"transactionType\":\"EXPENSE\"}}".formatted(debit)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].transactionTime").value("2026-09-05 10:00:00"))
                .andExpect(jsonPath("$.data.records[0].categoryName").value("午餐"))
                .andExpect(jsonPath("$.data.records[0].accountName").value("工资卡"))
                .andExpect(jsonPath("$.data.records[0].balanceAfter").value(87.5));

        mockMvc.perform(post("/api/transactions/page").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    // ---------- helpers ----------

    private Long addAccountType(Long providerId, String code, String name) {
        AccountTypeSaveDTO dto = new AccountTypeSaveDTO();
        dto.setProviderId(providerId);
        dto.setTypeCode(code);
        dto.setTypeName(name);
        return Long.valueOf(accountTypeService.addAccountType(dto));
    }

    private Long addCategory(Long pid, String type, String name) {
        CategorySaveDTO dto = new CategorySaveDTO();
        dto.setPid(pid);
        dto.setCategoryType(type);
        dto.setCategoryName(name);
        return Long.valueOf(categoryService.addCategory(dto));
    }

    private CategoryQueryDTO query(String type) {
        CategoryQueryDTO query = new CategoryQueryDTO();
        query.setCategoryType(type);
        return query;
    }

    private AccountSaveDTO accountSaveDTO(Long typeId, String name) {
        AccountSaveDTO dto = new AccountSaveDTO();
        dto.setTypeId(typeId);
        dto.setAccName(name);
        return dto;
    }

    private Long addAccount(Long typeId, String name, String initialBalance, LocalDateTime time) {
        AccountSaveDTO dto = accountSaveDTO(typeId, name);
        dto.setInitialBalance(new BigDecimal(initialBalance));
        dto.setInitialTransactionTime(time);
        return Long.valueOf(accountService.addAccount(dto));
    }

    private void expense(TransactionSaveDTO dto, Long accountId, String amount, LocalDateTime time) {
        dto.setTransactionType("EXPENSE");
        dto.setAccountId(accountId);
        dto.setCategoryId(lunchCategoryId);
        dto.setAmount(new BigDecimal(amount));
        dto.setTransactionTime(time);
    }

    private void income(TransactionSaveDTO dto, Long accountId, String amount, LocalDateTime time) {
        dto.setTransactionType("INCOME");
        dto.setAccountId(accountId);
        dto.setCategoryId(salaryCategoryId);
        dto.setAmount(new BigDecimal(amount));
        dto.setTransactionTime(time);
    }

    private void transfer(TransactionSaveDTO dto, Long from, Long to, String amount, LocalDateTime time) {
        dto.setTransactionType("TRANSFER");
        dto.setAccountId(from);
        dto.setTargetAccountId(to);
        dto.setAmount(new BigDecimal(amount));
        dto.setTransactionTime(time);
    }

    private TransactionSaveDTO build(Consumer<TransactionSaveDTO> customizer) {
        TransactionSaveDTO dto = new TransactionSaveDTO();
        customizer.accept(dto);
        return dto;
    }

    private String saveTransaction(Consumer<TransactionSaveDTO> customizer) {
        return transactionService.saveTransaction(build(customizer));
    }

    private LocalDateTime time(int day, int hour) {
        return LocalDateTime.of(2026, 9, day, hour, 0);
    }

    private void assertBalance(Long accountId, String expected) {
        BigDecimal actual = accountService.getById(accountId).getBalance();
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "账户余额应为 " + expected + "，实际为 " + actual);
    }

    private void assertBalanceAfter(String transactionId, String expected) {
        BigDecimal actual = transactionService.getById(Long.valueOf(transactionId)).getBalanceAfter();
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "流水余额应为 " + expected + "，实际为 " + actual);
    }

    private List<String> closingBalances(Long accountId) {
        AccountDailyBalanceQueryDTO query = new AccountDailyBalanceQueryDTO();
        query.setAccountId(accountId);
        query.setStartDate(LocalDate.of(2026, 9, 1));
        return accountDailyBalanceService.listAccountDailyBalances(query).stream()
                .map(AccountDailyBalanceVO::getClosingBalance)
                .map(balance -> balance.setScale(2).toPlainString())
                .toList();
    }

    private void assertBizError(int code, Runnable action) {
        BizException exception = assertThrows(BizException.class, action::run);
        assertEquals(code, exception.getCode(), exception.getMessage());
    }
}
