package com.costcount.service.impl;

import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Category;
import com.costcount.exception.BizException;
import com.costcount.service.AccountService;
import com.costcount.service.BillImportService;
import com.costcount.service.CategoryService;
import com.costcount.service.TransactionRecordService;
import com.costcount.service.support.RapidOcrClient;
import com.costcount.service.support.RapidOcrClient.OcrText;
import com.costcount.vo.BillImportPreviewVO;
import com.costcount.vo.BillImportRowVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BillImportServiceImpl implements BillImportService {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("[-+]?\\d[\\d,]*\\.\\d{2}");
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2})[年/.-](\\d{1,2})[月/.-](\\d{1,2})");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd"), DateTimeFormatter.ofPattern("yyyy年M月d日")
    );
    @Resource
    private CategoryService categoryService;
    @Resource
    private AccountService accountService;
    @Resource
    private TransactionRecordService transactionRecordService;
    @Resource
    private RapidOcrClient rapidOcrClient;

    @Override
    public BillImportPreviewVO previewExcel(MultipartFile file) {
        validateExcel(file);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            BillImportPreviewVO preview = new BillImportPreviewVO();
            preview.setSource("EXCEL");
            preview.setFileCount(1);
            preview.setRows(parseExcel(sheet, loadLookups()));
            addWarnings(preview);
            return preview;
        } catch (BizException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            log.error("解析 Excel 账单失败", exception);
            throw new BizException(400, "Excel 解析失败，请检查文件格式和表头");
        }
    }

    @Override
    public BillImportPreviewVO recognizeImages(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BizException(400, "请选择账单截图");
        }
        ImportLookups lookups = loadLookups();
        BillImportPreviewVO preview = new BillImportPreviewVO();
        preview.setSource("OCR");
        preview.setFileCount(files.length);
        List<OcrText> recognizedTexts = rapidOcrClient.recognize(files);
        StringBuilder rawText = new StringBuilder();
        for (int index = 0; index < recognizedTexts.size(); index++) {
            OcrText recognized = recognizedTexts.get(index);
            String text = Optional.ofNullable(recognized.text()).orElse("");
            rawText.append("【").append(recognized.filename()).append("】\n").append(text).append('\n');
            preview.getRows().add(parseOcr(text, index + 1, recognized.filename(), lookups));
            if (StringUtils.hasText(recognized.error())) {
                preview.getWarnings().add(recognized.filename() + " 识别失败：" + recognized.error());
            }
        }
        preview.setRawText(rawText.toString().trim());
        addWarnings(preview);
        return preview;
    }

    @Override
    public List<String> confirm(List<TransactionSaveDTO> rows) {
        return transactionRecordService.batchCreate(rows, "IMPORT");
    }

    private List<BillImportRowVO> parseExcel(Sheet sheet, ImportLookups lookups) {
        if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
            throw new BizException(400, "Excel 中没有可导入的数据");
        }
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        Map<String, Integer> headers = readHeaders(sheet.getRow(sheet.getFirstRowNum()), formatter);
        requireColumns(headers);
        List<BillImportRowVO> rows = new ArrayList<>();
        for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && !isBlankRow(row, formatter)) {
                rows.add(parseExcelRow(row, rowIndex + 1, headers, lookups, formatter));
            }
        }
        return rows;
    }

    private BillImportRowVO parseExcelRow(Row row, int rowNumber, Map<String, Integer> headers,
                                          ImportLookups lookups, DataFormatter formatter) {
        BillImportRowVO result = new BillImportRowVO();
        result.setRowNumber(rowNumber);
        String typeText = text(row, column(headers, "收支类型", "类型", "收支"), formatter);
        String amountText = text(row, column(headers, "金额", "交易金额", "收支金额"), formatter);
        result.setType(parseType(typeText, amountText));
        result.setAmount(parseAmount(amountText));
        result.setTransactionDate(parseDate(row.getCell(column(headers, "交易日期", "日期", "交易时间")), formatter));
        result.setMerchant(text(row, column(headers, "交易对象", "商户", "商品", "对方"), formatter));
        result.setNote(text(row, column(headers, "备注", "说明"), formatter));
        matchCategory(result, text(row, column(headers, "分类", "收支分类"), formatter), lookups);
        matchAccount(result, text(row, column(headers, "账户", "资金账户", "支付方式"), formatter), lookups);
        result.setConfidence(100);
        validateRow(result);
        return result;
    }

    private BillImportRowVO parseOcr(String rawText, int rowNumber, String filename, ImportLookups lookups) {
        String normalized = Optional.ofNullable(rawText).orElse("").replaceAll("\\s+", "");
        BillImportRowVO row = new BillImportRowVO();
        row.setRowNumber(rowNumber);
        row.setType(normalized.contains("收入") || normalized.contains("退款") ? "INCOME" : "EXPENSE");
        row.setAmount(findAmount(normalized));
        row.setTransactionDate(findDate(normalized));
        row.setMerchant(findMerchant(rawText, filename));
        row.setNote("截图 OCR 导入");
        matchAccount(row, findContainedName(normalized, lookups.accountNames()), lookups);
        matchCategory(row, findCategoryName(normalized, row.getType(), lookups), lookups);
        row.setConfidence(calculateConfidence(row));
        validateRow(row);
        return row;
    }

    private Map<String, Integer> readHeaders(Row row, DataFormatter formatter) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        row.forEach(cell -> headers.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex()));
        return headers;
    }

    private void requireColumns(Map<String, Integer> headers) {
        if (column(headers, "交易日期", "日期", "交易时间") < 0
            || column(headers, "金额", "交易金额", "收支金额") < 0) {
            throw new BizException(400, "Excel 至少需要“交易日期”和“金额”两列");
        }
    }

    private int column(Map<String, Integer> headers, String... aliases) {
        for (String alias : aliases) {
            if (headers.containsKey(alias)) {
                return headers.get(alias);
            }
        }
        return -1;
    }

    private String text(Row row, int index, DataFormatter formatter) {
        if (index < 0 || row.getCell(index) == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (StringUtils.hasText(formatter.formatCellValue(cell))) {
                return false;
            }
        }
        return true;
    }

    private LocalDate parseDate(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = formatter.formatCellValue(cell).trim();
        String candidate = value.isEmpty() ? "" : value.split("\\s+")[0];
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(candidate, format);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种常见日期格式。
            }
        }
        return parseExcelSerialDate(candidate);
    }

    private LocalDate parseExcelSerialDate(String value) {
        try {
            int serialNumber = new BigDecimal(value).intValueExact();
            return serialNumber >= 1 ? LocalDate.of(1899, 12, 30).plusDays(serialNumber) : null;
        } catch (NumberFormatException | ArithmeticException exception) {
            return null;
        }
    }

    private String parseType(String type, String amount) {
        if (type.contains("收入") || type.equalsIgnoreCase("INCOME") || amount.startsWith("+")) {
            return "INCOME";
        }
        return "EXPENSE";
    }

    private BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(value.replace("¥", "").replace("￥", "").replace(",", "")
                .replace("+", "").trim()).abs().setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal findAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        BigDecimal largest = null;
        while (matcher.find()) {
            BigDecimal candidate = parseAmount(matcher.group());
            if (candidate != null && (largest == null || candidate.compareTo(largest) > 0)) {
                largest = candidate;
            }
        }
        return largest;
    }

    private LocalDate findDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)));
        } catch (DateTimeException exception) {
            return null;
        }
    }

    private String findMerchant(String text, String filename) {
        return Optional.ofNullable(text).stream().flatMap(String::lines)
            .map(String::trim).filter(line -> line.matches(".*[\\p{IsHan}A-Za-z]{2,}.*"))
            .filter(line -> !line.matches(".*(支付成功|交易详情|付款方式|交易时间|订单号|金额).*"))
            .findFirst().orElseGet(() -> Optional.ofNullable(filename).orElse("截图识别")
                .replaceFirst("\\.[^.]+$", ""));
    }

    private String findCategoryName(String text, String type, ImportLookups lookups) {
        return lookups.categoryCandidates().stream()
            .filter(category -> type.equals(category.getType()) && text.contains(category.getName()))
            .map(Category::getName)
            .findFirst()
            .orElse("");
    }

    private void matchCategory(BillImportRowVO row, String name, ImportLookups lookups) {
        row.setCategoryName(name);
        if (StringUtils.hasText(name)) {
            Optional.ofNullable(lookups.categories().get(row.getType() + "|" + name))
                .ifPresent(category -> row.setCategoryId(category.getId()));
        }
    }

    private void matchAccount(BillImportRowVO row, String name, ImportLookups lookups) {
        row.setAccountName(name);
        if (StringUtils.hasText(name)) {
            Optional.ofNullable(lookups.accounts().get(name)).ifPresent(account -> row.setAccountId(account.getId()));
        }
    }

    private ImportLookups loadLookups() {
        List<Category> categoryList = Optional.ofNullable(categoryService.list()).orElseGet(List::of);
        Set<Long> parentIds = categoryList.stream().map(Category::getParentId).filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<Category> categoryCandidates = categoryList.stream()
            .filter(category -> !parentIds.contains(category.getId()))
            .filter(category -> StringUtils.hasText(category.getName()))
            .sorted(Comparator.comparingInt((Category category) -> category.getName().length()).reversed()
                .thenComparing(category -> Optional.ofNullable(category.getSort()).orElse(Integer.MAX_VALUE))
                .thenComparing(category -> Optional.ofNullable(category.getId()).orElse(Long.MAX_VALUE)))
            .toList();
        Map<String, Category> categories = categoryCandidates.stream()
            .collect(Collectors.toMap(category -> category.getType() + "|" + category.getName(),
                Function.identity(), (left, right) -> left));
        List<Account> accountList = Optional.ofNullable(accountService.list()).orElseGet(List::of);
        Map<String, Account> accounts = new LinkedHashMap<>();
        accountList.forEach(account -> accounts.put(account.getName(), account));
        List<String> accountNames = accountList.stream()
            .map(Account::getName)
            .filter(StringUtils::hasText)
            .distinct()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();
        return new ImportLookups(categories, accounts, accountNames, categoryCandidates);
    }

    private String findContainedName(String text, List<String> names) {
        return names.stream().filter(text::contains).findFirst().orElse("");
    }

    private int calculateConfidence(BillImportRowVO row) {
        int score = 25;
        score += row.getAmount() == null ? 0 : 30;
        score += row.getAccountId() == null ? 0 : 20;
        score += row.getCategoryId() == null ? 0 : 15;
        score += StringUtils.hasText(row.getMerchant()) ? 10 : 0;
        return score;
    }

    private void validateRow(BillImportRowVO row) {
        List<String> errors = new ArrayList<>();
        if (row.getAmount() == null || row.getAmount().signum() <= 0) {
            errors.add("请修正金额");
        }
        if (row.getTransactionDate() == null) {
            errors.add("请修正交易日期");
        }
        if (!StringUtils.hasText(row.getMerchant())) {
            errors.add("请填写交易对象");
        }
        if (row.getCategoryId() == null) {
            errors.add("请选择二级分类");
        }
        if (row.getAccountId() == null) {
            errors.add("请选择资金账户");
        }
        row.setValid(errors.isEmpty());
        row.setErrorMessage(String.join("；", errors));
    }

    private void addWarnings(BillImportPreviewVO preview) {
        long invalidCount = preview.getRows().stream().filter(row -> !Boolean.TRUE.equals(row.getValid())).count();
        if (invalidCount > 0) {
            preview.getWarnings().add(invalidCount + " 行需要补充账户、分类或金额后才能导入");
        }
        if (preview.getRows().isEmpty()) {
            preview.getWarnings().add("没有识别到可导入的账目");
        }
    }

    private void validateExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择 Excel 文件");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("账单.xlsx");
        if (!filename.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls)$")) {
            throw new BizException(400, "仅支持 .xlsx 或 .xls 文件");
        }
    }

    private record ImportLookups(Map<String, Category> categories, Map<String, Account> accounts,
                                 List<String> accountNames, List<Category> categoryCandidates) {
    }
}
