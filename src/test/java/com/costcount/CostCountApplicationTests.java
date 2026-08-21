package com.costcount;

import com.costcount.dto.AccountSaveDTO;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Category;
import com.costcount.service.AccountService;
import com.costcount.service.BillImportService;
import com.costcount.service.CategoryService;
import com.costcount.service.TransactionRecordService;
import jakarta.annotation.Resource;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CostCountApplicationTests {
    @Resource
    private AccountService accountService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private TransactionRecordService transactionRecordService;
    @Resource
    private BillImportService billImportService;

    @Test
    void shouldInitializeCommonCategories() {
        assertThat(categoryService.count()).isGreaterThanOrEqualTo(12);
    }

    @Test
    void shouldAdjustAndRestoreBalanceWithExpense() {
        AccountSaveDTO accountDTO = new AccountSaveDTO();
        accountDTO.setType("现金");
        accountDTO.setBalance(new BigDecimal("100.00"));
        accountDTO.setColor("#3154E5");
        Account account = accountService.getById(Long.valueOf(accountService.create(accountDTO)));
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

    @Test
    void shouldIncreaseLiabilityWhenPayingByCreditAccount() {
        Account creditAccount = createAccount("招商银行信用卡", "200.00");
        TransactionSaveDTO dto = new TransactionSaveDTO();
        dto.setType("EXPENSE");
        dto.setAmount(new BigDecimal("50.00"));
        dto.setCategoryId(categoryService.lambdaQuery().eq(Category::getType, "EXPENSE").list().getFirst().getId());
        dto.setAccountId(creditAccount.getId());
        dto.setTransactionDate(LocalDate.now());
        dto.setMerchant("信用卡消费测试");

        String id = transactionRecordService.create(dto);

        assertThat(creditAccount.getNature()).isEqualTo("LIABILITY");
        assertThat(accountService.getById(creditAccount.getId()).getBalance()).isEqualByComparingTo("250.00");
        transactionRecordService.delete(Long.valueOf(id));
        assertThat(accountService.getById(creditAccount.getId()).getBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldTransferFromAssetToLiabilityAndRestoreBothBalances() {
        Account bankAccount = createAccount("招商银行", "1000.00");
        Account creditAccount = createAccount("招商银行信用卡", "200.00");
        TransactionSaveDTO dto = new TransactionSaveDTO();
        dto.setType("TRANSFER");
        dto.setAmount(new BigDecimal("150.00"));
        dto.setAccountId(bankAccount.getId());
        dto.setTargetAccountId(creditAccount.getId());
        dto.setTransactionDate(LocalDate.now());
        dto.setMerchant("账户转账");

        String id = transactionRecordService.create(dto);

        assertThat(accountService.getById(bankAccount.getId()).getBalance()).isEqualByComparingTo("850.00");
        assertThat(accountService.getById(creditAccount.getId()).getBalance()).isEqualByComparingTo("50.00");
        transactionRecordService.delete(Long.valueOf(id));
        assertThat(accountService.getById(bankAccount.getId()).getBalance()).isEqualByComparingTo("1000.00");
        assertThat(accountService.getById(creditAccount.getId()).getBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldPreviewExcelBillWithoutWritingTransactions() throws Exception {
        byte[] content;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("账单");
            var header = sheet.createRow(0);
            List.of("交易日期", "收支类型", "金额", "分类", "账户", "交易对象", "备注")
                .forEach(value -> header.createCell(header.getLastCellNum() < 0 ? 0 : header.getLastCellNum()).setCellValue(value));
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026-08-21");
            row.createCell(1).setCellValue("支出");
            row.createCell(2).setCellValue(36.50);
            row.createCell(3).setCellValue("午餐");
            row.createCell(4).setCellValue("微信");
            row.createCell(5).setCellValue("测试餐厅");
            row.createCell(6).setCellValue("Excel 预览测试");
            workbook.write(output);
            content = output.toByteArray();
        }
        MockMultipartFile file = new MockMultipartFile("file", "账单.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        var preview = billImportService.previewExcel(file);

        assertThat(preview.getRows()).hasSize(1);
        assertThat(preview.getRows().getFirst().getAmount()).isEqualByComparingTo("36.50");
        assertThat(preview.getRows().getFirst().getCategoryName()).isEqualTo("午餐");
        assertThat(transactionRecordService.count()).isZero();
    }

    private Account createAccount(String type, String balance) {
        AccountSaveDTO dto = new AccountSaveDTO();
        dto.setType(type);
        dto.setBalance(new BigDecimal(balance));
        dto.setColor("#3154E5");
        return accountService.getById(Long.valueOf(accountService.create(dto)));
    }
}
