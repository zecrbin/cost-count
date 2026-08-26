package com.costcount;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.costcount.dto.AccountSaveDTO;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Category;
import com.costcount.exception.BizException;
import com.costcount.service.AccountService;
import com.costcount.service.BillImportService;
import com.costcount.service.CategoryService;
import com.costcount.service.TransactionRecordService;
import com.costcount.service.support.BillImportRowMapper;
import jakarta.annotation.Resource;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @Resource
    private BillImportRowMapper billImportRowMapper;

    @Test
    void shouldInitializeCommonCategories() {
        assertThat(categoryService.count()).isGreaterThanOrEqualTo(12);
    }

    @Test
    void shouldAdjustAndRestoreBalanceWithExpense() {
        Account account = createAccount("现金", "100.00");
        BigDecimal originalBalance = account.getBalance();
        TransactionSaveDTO dto = new TransactionSaveDTO();
        dto.setType("EXPENSE");
        dto.setAmount(new BigDecimal("12.34"));
        dto.setCategoryId(expenseCategoryId());
        dto.setAccountId(account.getId());
        dto.setTransactionDate(LocalDate.now());
        dto.setMerchant("集成测试");
        String id = transactionRecordService.create(dto);

        assertThat(accountService.getById(account.getId()).getBalance())
            .isEqualByComparingTo(originalBalance.subtract(dto.getAmount()));
        transactionRecordService.delete(Long.valueOf(id));
        assertThat(accountService.getById(account.getId()).getBalance()).isEqualByComparingTo(originalBalance);
    }

    @Test
    void shouldIncreaseLiabilityWhenPayingByCreditAccount() {
        Account creditAccount = createAccount("招商银行信用卡", "200.00");
        TransactionSaveDTO dto = new TransactionSaveDTO();
        dto.setType("EXPENSE");
        dto.setAmount(new BigDecimal("50.00"));
        dto.setCategoryId(expenseCategoryId());
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
    void shouldPreviewExcelBillWithoutWritingTransactions() {
        List<String> columns = List.of("交易日期", "收支类型", "金额", "分类", "账户", "交易对象", "备注");
        List<Object> values = List.of(LocalDate.of(2026, 8, 21), "支出", 36.50, "午餐", "微信",
            "测试餐厅", "Excel 预览测试");
        MockMultipartFile file = createExcelFile(columns, values);

        var preview = billImportService.previewExcel(file);

        assertThat(preview.getRows()).hasSize(1);
        assertThat(preview.getRows().getFirst().getAmount()).isEqualByComparingTo("36.50");
        assertThat(preview.getRows().getFirst().getTransactionDate()).isEqualTo(LocalDate.of(2026, 8, 21));
        assertThat(preview.getRows().getFirst().getCategoryName()).isEqualTo("午餐");
        assertThat(transactionRecordService.count()).isZero();
    }

    @Test
    void shouldRejectExcelWithoutRequiredColumns() {
        MockMultipartFile file = createExcelFile(List.of("金额", "交易对象"), List.of(12.50, "测试商户"));

        assertThatThrownBy(() -> billImportService.previewExcel(file))
            .isInstanceOf(BizException.class)
            .hasMessage("Excel 至少需要“交易日期”和“金额”两列");
    }

    @Test
    void shouldPreviewLegacyXlsBill() {
        MockMultipartFile file = createExcelFile(List.of("交易日期", "金额", "交易对象"),
            List.of("2026-08-21", 18.80, "测试商户"), "账单.xls", ExcelTypeEnum.XLS);

        var preview = billImportService.previewExcel(file);

        assertThat(preview.getRows()).hasSize(1);
        assertThat(preview.getRows().getFirst().getAmount()).isEqualByComparingTo("18.80");
    }

    @Test
    void shouldPreferSpecificOcrCategoryKeyword() {
        var rows = billImportRowMapper.mapOcr(List.of(
            new BillImportRowMapper.OcrText(1, "外卖账单.png", "支付成功\n外卖餐费\n金额 36.50")));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getCategoryName()).isEqualTo("外卖");
        assertThat(rows.getFirst().getCategoryId()).isNotNull();
    }

    @Test
    void shouldRejectEmptyOrUnsupportedExcelFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "账单.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        MockMultipartFile unsupportedFile = new MockMultipartFile("file", "账单.csv", "text/csv",
            "交易日期,金额".getBytes());

        assertThatThrownBy(() -> billImportService.previewExcel(emptyFile))
            .isInstanceOf(BizException.class)
            .hasMessage("请选择 Excel 文件");
        assertThatThrownBy(() -> billImportService.previewExcel(unsupportedFile))
            .isInstanceOf(BizException.class)
            .hasMessage("仅支持 .xlsx 或 .xls 文件");
    }

    private Account createAccount(String type, String balance) {
        AccountSaveDTO dto = new AccountSaveDTO();
        dto.setType(type);
        dto.setBalance(new BigDecimal(balance));
        dto.setColor("#3154E5");
        return accountService.getById(Long.valueOf(accountService.create(dto)));
    }

    private Long expenseCategoryId() {
        return categoryService.lambdaQuery().eq(Category::getType, "EXPENSE")
            .orderByAsc(Category::getId).list().getFirst().getId();
    }

    private MockMultipartFile createExcelFile(List<String> columns, List<Object> values) {
        return createExcelFile(columns, values, "账单.xlsx", ExcelTypeEnum.XLSX);
    }

    private MockMultipartFile createExcelFile(List<String> columns, List<Object> values, String filename,
                                              ExcelTypeEnum excelType) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output).excelType(excelType).head(columns.stream().map(List::of).toList()).sheet("账单")
            .doWrite(List.of(values));
        return new MockMultipartFile("file", filename, "application/octet-stream", output.toByteArray());
    }
}
