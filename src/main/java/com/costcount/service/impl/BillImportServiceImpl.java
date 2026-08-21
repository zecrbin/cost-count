package com.costcount.service.impl;

import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Category;
import com.costcount.exception.BizException;
import com.costcount.service.AccountService;
import com.costcount.service.BillImportService;
import com.costcount.service.CategoryService;
import com.costcount.service.TransactionRecordService;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    private static final Map<String, String> CATEGORY_KEYWORDS = Map.ofEntries(
        Map.entry("外卖", "外卖"), Map.entry("餐", "午餐"), Map.entry("咖啡", "咖啡茶饮"),
        Map.entry("地铁", "公交地铁"), Map.entry("公交", "公交地铁"), Map.entry("打车", "打车"),
        Map.entry("滴滴", "打车"), Map.entry("加油", "加油"), Map.entry("停车", "停车"),
        Map.entry("药", "药品"), Map.entry("医院", "挂号问诊"), Map.entry("超市", "日用品"),
        Map.entry("话费", "手机通讯"), Map.entry("会员", "会员订阅"), Map.entry("工资", "工资"),
        Map.entry("退款", "退款返还")
    );

    @Resource
    private CategoryService categoryService;
    @Resource
    private AccountService accountService;
    @Resource
    private TransactionRecordService transactionRecordService;

    @Override
    public BillImportPreviewVO previewExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择 Excel 文件");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("账单.xlsx");
        if (!filename.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls)$")) {
            throw new BizException(400, "仅支持 .xlsx 或 .xls 文件");
        }
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            BillImportPreviewVO preview = new BillImportPreviewVO();
            preview.setSource("EXCEL");
            preview.setFileCount(1);
            preview.setRows(parseSheet(workbook.getSheetAt(0), loadLookups()));
            addPreviewWarnings(preview);
            return preview;
        } catch (Exception exception) {
            log.error("解析 Excel 账单失败", exception);
            throw new BizException(400, "Excel 解析失败，请检查文件格式和表头");
        }
    }

    @Override
    public BillImportPreviewVO recognizeImages(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BizException(400, "请选择账单截图");
        }
        BillImportPreviewVO preview = new BillImportPreviewVO();
        preview.setSource("OCR");
        preview.setFileCount(files.length);
        ImportLookups lookups = loadLookups();
        StringBuilder rawText = new StringBuilder();
        for (int index = 0; index < files.length; index++) {
            String text = recognize(files[index]);
            rawText.append("【").append(files[index].getOriginalFilename()).append("】\n").append(text).append("\n");
            preview.getRows().add(parseOcrRow(text, index + 1, files[index].getOriginalFilename(), lookups));
        }
        preview.setRawText(rawText.toString().trim());
        addPreviewWarnings(preview);
        return preview;
    }

    @Override
    public List<String> confirm(List<TransactionSaveDTO> rows) {
        return transactionRecordService.batchCreate(rows, "IMPORT");
    }

    private List<BillImportRowVO> parseSheet(Sheet sheet, ImportLookups lookups) {
        if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
            throw new BizException(400, "Excel 中没有可导入的数据");
        }
        Map<String, Integer> headers = readHeaders(sheet.getRow(sheet.getFirstRowNum()));
        requireColumns(headers);
        List<BillImportRowVO> rows = new ArrayList<>();
        for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && !isBlankRow(row)) {
                rows.add(parseExcelRow(row, rowIndex + 1, headers, lookups));
            }
        }
        return rows;
    }

    private BillImportRowVO parseExcelRow(Row row, int rowNumber, Map<String, Integer> headers, ImportLookups lookups) {
        BillImportRowVO result = new BillImportRowVO();
        result.setRowNumber(rowNumber);
        String typeText = text(row, column(headers, "收支类型", "类型", "收支"));
        String amountText = text(row, column(headers, "金额", "交易金额", "收支金额"));
        result.setType(parseType(typeText, amountText));
        result.setAmount(parseAmount(amountText));
        result.setTransactionDate(parseDate(row.getCell(column(headers, "交易日期", "日期", "交易时间"))));
        result.setMerchant(text(row, column(headers, "交易对象", "商户", "商品", "对方")));
        result.setNote(text(row, column(headers, "备注", "说明")));
        matchCategory(result, text(row, column(headers, "分类", "收支分类")), lookups);
        matchAccount(result, text(row, column(headers, "账户", "资金账户", "支付方式")), lookups);
        result.setConfidence(100);
        validateRow(result);
        return result;
    }

    private BillImportRowVO parseOcrRow(String rawText, int rowNumber, String filename, ImportLookups lookups) {
        String normalized = Optional.ofNullable(rawText).orElse("").replaceAll("\\s+", "");
        BillImportRowVO row = new BillImportRowVO();
        row.setRowNumber(rowNumber);
        row.setType(normalized.contains("收入") || normalized.contains("退款") ? "INCOME" : "EXPENSE");
        row.setAmount(findAmount(normalized));
        row.setTransactionDate(findDate(normalized));
        row.setMerchant(findMerchant(rawText, filename));
        row.setNote("截图 OCR 导入");
        matchAccount(row, findContainedName(normalized, lookups.accountNames()), lookups);
        String categoryName = CATEGORY_KEYWORDS.entrySet().stream().filter(entry -> normalized.contains(entry.getKey()))
            .map(Map.Entry::getValue).findFirst().orElse("");
        matchCategory(row, categoryName, lookups);
        row.setConfidence(calculateConfidence(row));
        validateRow(row);
        return row;
    }

    private String recognize(MultipartFile file) {
        String suffix = Optional.ofNullable(file.getOriginalFilename()).filter(name -> name.contains("."))
            .map(name -> name.substring(name.lastIndexOf('.'))).orElse(".png");
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("cost-count-ocr-", suffix);
            file.transferTo(tempFile);
            Path script = resolveOcrScript();
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", script.toString(), "-ImagePath", tempFile.toString()).redirectErrorStream(true).start();
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new BizException(408, "OCR 识别超时");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0 || !StringUtils.hasText(output)) {
                throw new BizException(500, "Windows OCR 未能识别该图片");
            }
            return output;
        } catch (IOException exception) {
            log.error("调用 Windows OCR 失败", exception);
            throw new BizException(500, "无法启动本机 OCR 服务");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("等待 Windows OCR 时被中断", exception);
            throw new BizException(500, "OCR 识别被中断");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException exception) {
                    log.error("删除 OCR 临时图片失败", exception);
                }
            }
        }
    }

    private Path resolveOcrScript() {
        List<Path> candidates = List.of(Path.of("scripts", "windows-ocr.ps1"), Path.of("backend", "scripts", "windows-ocr.ps1"));
        return candidates.stream().map(Path::toAbsolutePath).filter(Files::exists).findFirst()
            .orElseThrow(() -> new BizException(500, "找不到 Windows OCR 脚本"));
    }

    private Map<String, Integer> readHeaders(Row row) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        row.forEach(cell -> headers.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex()));
        return headers;
    }

    private void requireColumns(Map<String, Integer> headers) {
        if (column(headers, "交易日期", "日期", "交易时间") < 0 || column(headers, "金额", "交易金额", "收支金额") < 0) {
            throw new BizException(400, "Excel 至少需要“交易日期”和“金额”两列");
        }
    }

    private int column(Map<String, Integer> headers, String... aliases) {
        return Arrays.stream(aliases).filter(headers::containsKey).map(headers::get).findFirst().orElse(-1);
    }

    private String text(Row row, int index) {
        if (index < 0 || row.getCell(index) == null) {
            return "";
        }
        return new DataFormatter(Locale.CHINA).formatCellValue(row.getCell(index)).trim();
    }

    private boolean isBlankRow(Row row) {
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        for (Cell cell : row) {
            if (StringUtils.hasText(formatter.formatCellValue(cell))) {
                return false;
            }
        }
        return true;
    }

    private LocalDate parseDate(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = new DataFormatter(Locale.CHINA).formatCellValue(cell).trim();
        return DATE_FORMATS.stream().map(format -> {
            try {
                return LocalDate.parse(value.substring(0, Math.min(value.length(), 10)), format);
            } catch (DateTimeParseException | IndexOutOfBoundsException ignored) {
                return null;
            }
        }).filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }

    private String parseType(String type, String amount) {
        if (type.contains("收入") || type.equalsIgnoreCase("INCOME") || amount.startsWith("+")) {
            return "INCOME";
        }
        return "EXPENSE";
    }

    private BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(value.replace("¥", "").replace("￥", "").replace(",", "").replace("+", "").trim()).abs().setScale(2, RoundingMode.HALF_UP);
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
        return matcher.find() ? LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))) : LocalDate.now();
    }

    private String findMerchant(String text, String filename) {
        return Optional.ofNullable(text).stream().flatMap(value -> value.lines())
            .map(String::trim).filter(line -> line.matches(".*[\\p{IsHan}A-Za-z]{2,}.*"))
            .filter(line -> !line.matches(".*(支付成功|交易详情|付款方式|交易时间|订单号|金额).*"))
            .findFirst().orElseGet(() -> Optional.ofNullable(filename).orElse("截图识别").replaceFirst("\\.[^.]+$", ""));
    }

    private void matchCategory(BillImportRowVO row, String name, ImportLookups lookups) {
        row.setCategoryName(name);
        if (!StringUtils.hasText(name)) {
            return;
        }
        Optional.ofNullable(lookups.categories().get(row.getType() + "|" + name))
            .ifPresent(category -> row.setCategoryId(category.getId()));
    }

    private void matchAccount(BillImportRowVO row, String name, ImportLookups lookups) {
        row.setAccountName(name);
        if (!StringUtils.hasText(name)) {
            return;
        }
        Optional.ofNullable(lookups.accounts().get(name)).ifPresent(account -> row.setAccountId(account.getId()));
    }

    private ImportLookups loadLookups() {
        List<Category> categoryList = categoryService.list();
        Set<Long> parentIds = categoryList.stream().map(Category::getParentId)
            .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<String, Category> categories = categoryList.stream().filter(category -> !parentIds.contains(category.getId()))
            .collect(Collectors.toMap(category -> category.getType() + "|" + category.getName(), Function.identity(), (left, right) -> left));
        List<Account> accountList = accountService.list();
        Map<String, Account> accounts = new LinkedHashMap<>();
        accountList.forEach(account -> {
            accounts.put(account.getName(), account);
            accounts.put(account.getType(), account);
        });
        List<String> accountNames = accountList.stream().map(Account::getType).filter(StringUtils::hasText).toList();
        return new ImportLookups(categories, accounts, accountNames);
    }

    private String findContainedName(String text, List<String> names) {
        return names.stream().filter(text::contains).findFirst().orElse("");
    }

    private int calculateConfidence(BillImportRowVO row) {
        int score = 25;
        if (row.getAmount() != null) score += 30;
        if (row.getAccountId() != null) score += 20;
        if (row.getCategoryId() != null) score += 15;
        if (StringUtils.hasText(row.getMerchant())) score += 10;
        return score;
    }

    private void validateRow(BillImportRowVO row) {
        List<String> errors = new ArrayList<>();
        if (row.getAmount() == null || row.getAmount().compareTo(BigDecimal.ZERO) <= 0) errors.add("请修正金额");
        if (row.getTransactionDate() == null) errors.add("请修正交易日期");
        if (!StringUtils.hasText(row.getMerchant())) errors.add("请填写交易对象");
        if (row.getCategoryId() == null) errors.add("请选择二级分类");
        if (row.getAccountId() == null) errors.add("请选择资金账户");
        row.setValid(errors.isEmpty());
        row.setErrorMessage(String.join("；", errors));
    }

    private void addPreviewWarnings(BillImportPreviewVO preview) {
        long invalid = preview.getRows().stream().filter(row -> !Boolean.TRUE.equals(row.getValid())).count();
        if (invalid > 0) {
            preview.getWarnings().add(invalid + " 行需要补充账户、分类或金额后才能导入");
        }
        if (preview.getRows().isEmpty()) {
            preview.getWarnings().add("没有识别到可导入的账目");
        }
    }

    private record ImportLookups(Map<String, Category> categories, Map<String, Account> accounts, List<String> accountNames) {
    }
}
