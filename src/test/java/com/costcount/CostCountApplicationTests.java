package com.costcount;

import com.costcount.dto.AccountSaveDTO;
import com.costcount.dto.CategorySaveDTO;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Category;
import com.costcount.enums.AccountNature;
import com.costcount.exception.BizException;
import com.costcount.service.AccountService;
import com.costcount.service.BillImportService;
import com.costcount.service.CategoryService;
import com.costcount.service.TransactionRecordService;
import com.costcount.service.support.RapidOcrClient;
import com.costcount.service.support.RapidOcrClient.OcrText;
import jakarta.annotation.Resource;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @MockitoBean
    private RapidOcrClient rapidOcrClient;

    @Test
    void shouldInitializeCommonCategories() {
        assertThat(categoryService.count()).isGreaterThanOrEqualTo(12);
    }

    @Test
    void shouldAdjustAndRestoreBalanceWithExpense() {
        Account account = createAccount("日常资产账户", AccountNature.ASSET, "100.00");
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
        Account creditAccount = createAccount("自定义消费贷", AccountNature.LIABILITY, "200.00");
        TransactionSaveDTO dto = new TransactionSaveDTO();
        dto.setType("EXPENSE");
        dto.setAmount(new BigDecimal("50.00"));
        dto.setCategoryId(expenseCategoryId());
        dto.setAccountId(creditAccount.getId());
        dto.setTransactionDate(LocalDate.now());
        dto.setMerchant("信用卡消费测试");

        String id = transactionRecordService.create(dto);

        assertThat(creditAccount.getNature()).isEqualTo(AccountNature.LIABILITY);
        assertThat(accountService.getById(creditAccount.getId()).getBalance()).isEqualByComparingTo("250.00");
        transactionRecordService.delete(Long.valueOf(id));
        assertThat(accountService.getById(creditAccount.getId()).getBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldTransferFromAssetToLiabilityAndRestoreBothBalances() {
        Account bankAccount = createAccount("工资账户", AccountNature.ASSET, "1000.00");
        Account creditAccount = createAccount("分期账户", AccountNature.LIABILITY, "200.00");
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
            List.of("2026-08-21", 18.80, "测试商户"), "账单.xls", true);

        var preview = billImportService.previewExcel(file);

        assertThat(preview.getRows()).hasSize(1);
        assertThat(preview.getRows().getFirst().getAmount()).isEqualByComparingTo("18.80");
    }

    @Test
    void shouldUseDatabaseCategoryForExcelMatching() {
        CategorySaveDTO category = new CategorySaveDTO();
        category.setName("宠物用品");
        category.setType("EXPENSE");
        String categoryId = categoryService.create(category);

        MockMultipartFile file = createExcelFile(List.of("交易日期", "金额", "分类", "交易对象"),
            List.of("2026-08-21", 36.50, "宠物用品", "宠物用品消费"));
        var preview = billImportService.previewExcel(file);

        assertThat(preview.getRows()).hasSize(1);
        assertThat(preview.getRows().getFirst().getCategoryName()).isEqualTo("宠物用品");
        assertThat(preview.getRows().getFirst().getCategoryId()).isEqualTo(Long.valueOf(categoryId));

        category.setName("宠物食品");
        categoryService.update(Long.valueOf(categoryId), category);
        file = createExcelFile(List.of("交易日期", "金额", "分类", "交易对象"),
            List.of("2026-08-21", 36.50, "宠物食品", "宠物食品消费"));
        var updatedPreview = billImportService.previewExcel(file);

        assertThat(updatedPreview.getRows().getFirst().getCategoryName()).isEqualTo("宠物食品");
        assertThat(updatedPreview.getRows().getFirst().getCategoryId()).isEqualTo(Long.valueOf(categoryId));
    }

    @Test
    void shouldRecognizeImageBatchWithSingleWorkerCall() {
        MultipartFile[] files = {
            new MockMultipartFile("files", "第一张.png", "image/png", new byte[]{1}),
            new MockMultipartFile("files", "第二张.png", "image/png", new byte[]{2})
        };
        when(rapidOcrClient.recognize(files)).thenReturn(List.of(
            new OcrText("第一张.png", "支付成功\n金额 12.50", ""),
            new OcrText("第二张.png", "支付成功\n金额 36.50", "")
        ));

        var preview = billImportService.recognizeImages(files);

        assertThat(preview.getFileCount()).isEqualTo(2);
        assertThat(preview.getRows()).hasSize(2);
        assertThat(preview.getRows()).extracting(row -> row.getAmount().toPlainString())
            .containsExactly("12.50", "36.50");
        verify(rapidOcrClient).recognize(files);
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

    private Account createAccount(String name, AccountNature nature, String balance) {
        AccountSaveDTO dto = new AccountSaveDTO();
        dto.setName(name);
        dto.setNature(nature);
        dto.setBalance(new BigDecimal(balance));
        dto.setColor("#3154E5");
        return accountService.getById(Long.valueOf(accountService.create(dto)));
    }

    private Long expenseCategoryId() {
        return categoryService.lambdaQuery().eq(Category::getType, "EXPENSE")
            .orderByAsc(Category::getId).list().getFirst().getId();
    }

    private MockMultipartFile createExcelFile(List<String> columns, List<Object> values) {
        return createExcelFile(columns, values, "账单.xlsx", false);
    }

    private MockMultipartFile createExcelFile(List<String> columns, List<Object> values, String filename,
                                              boolean legacyXls) {
        try (Workbook workbook = legacyXls ? new HSSFWorkbook() : new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("账单");
            Row header = sheet.createRow(0);
            for (int index = 0; index < columns.size(); index++) {
                header.createCell(index).setCellValue(columns.get(index));
            }
            Row data = sheet.createRow(1);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd"));
            for (int index = 0; index < values.size(); index++) {
                Cell cell = data.createCell(index);
                Object value = values.get(index);
                if (value instanceof LocalDate date) {
                    cell.setCellValue(date);
                    cell.setCellStyle(dateStyle);
                } else if (value instanceof Number number) {
                    cell.setCellValue(number.doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(value));
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return new MockMultipartFile("file", filename, "application/octet-stream", output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("生成测试 Excel 失败", exception);
        }
    }
}
