package com.costcount.service.impl;

import com.costcount.dto.TransactionPageQuery;
import com.costcount.dto.TransactionQueryDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Category;
import com.costcount.entity.TransactionRecord;
import com.costcount.service.AccountService;
import com.costcount.service.CategoryService;
import com.costcount.service.DashboardService;
import com.costcount.service.TransactionRecordService;
import com.costcount.vo.CategoryStatVO;
import com.costcount.vo.DashboardVO;
import com.costcount.vo.TransactionVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {
    @Resource
    private AccountService accountService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private TransactionRecordService transactionRecordService;

    @Override
    public DashboardVO getSummary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        List<TransactionRecord> records = transactionRecordService.lambdaQuery()
            .ge(TransactionRecord::getTransactionDate, monthStart)
            .le(TransactionRecord::getTransactionDate, today)
            .list();
        List<TransactionRecord> safeRecords = Optional.ofNullable(records).orElseGet(List::of);
        BigDecimal income = sumByType(safeRecords, "INCOME");
        BigDecimal expense = sumByType(safeRecords, "EXPENSE");
        List<Account> accounts = accountService.lambdaQuery().list();
        BigDecimal totalBalance = Optional.ofNullable(accounts).orElseGet(List::of).stream()
            .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Category> categories = categoryService.lambdaQuery().list();
        Map<Long, Category> categoryMap = Optional.ofNullable(categories).orElseGet(List::of).stream()
            .collect(Collectors.toMap(Category::getId, item -> item));
        List<CategoryStatVO> stats = safeRecords.stream().filter(item -> "EXPENSE".equals(item.getType()))
            .collect(Collectors.groupingBy(TransactionRecord::getCategoryId,
                Collectors.reducing(BigDecimal.ZERO, TransactionRecord::getAmount, BigDecimal::add)))
            .entrySet().stream().map(entry -> toCategoryStat(entry.getKey(), entry.getValue(), expense, categoryMap))
            .sorted((left, right) -> right.amount().compareTo(left.amount())).toList();
        TransactionPageQuery recentQuery = new TransactionPageQuery();
        recentQuery.setPageSize(8);
        recentQuery.setParams(new TransactionQueryDTO());
        List<TransactionVO> recent = transactionRecordService.pageList(recentQuery).records();
        return new DashboardVO(totalBalance, income, expense, income.subtract(expense), safeRecords.size(), stats, recent);
    }

    private BigDecimal sumByType(List<TransactionRecord> records, String type) {
        return records.stream().filter(item -> type.equals(item.getType())).map(TransactionRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CategoryStatVO toCategoryStat(Long id, BigDecimal amount, BigDecimal total, Map<Long, Category> categories) {
        Category category = categories.get(id);
        String name = category == null ? "未分类" : category.getName();
        String color = category == null ? "#D9DCE4" : category.getColor();
        BigDecimal percent = total.signum() == 0 ? BigDecimal.ZERO : amount.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
        return new CategoryStatVO(name, color, amount, percent);
    }
}
