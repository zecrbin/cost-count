package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.costcount.dto.TransactionPageQuery;
import com.costcount.dto.TransactionQueryDTO;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Category;
import com.costcount.entity.TransactionRecord;
import com.costcount.enums.AccountNature;
import com.costcount.enums.TransactionType;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.CategoryMapper;
import com.costcount.mapper.TransactionRecordMapper;
import com.costcount.service.TransactionRecordService;
import com.costcount.service.AccountService;
import com.costcount.vo.PageResult;
import com.costcount.vo.TransactionVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransactionRecordServiceImpl extends MPJBaseServiceImpl<TransactionRecordMapper, TransactionRecord> implements TransactionRecordService {
    @Resource
    private AccountMapper accountMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private AccountService accountService;

    @Override
    public PageResult<TransactionVO> pageList(TransactionPageQuery query) {
        TransactionQueryDTO params = query.getParams();
        MPJLambdaWrapper<TransactionRecord> wrapper = new MPJLambdaWrapper<TransactionRecord>()
            .selectAll(TransactionRecord.class)
            .selectAs(Category::getName, TransactionVO::getCategoryName)
            .selectAs(Category::getColor, TransactionVO::getCategoryColor)
            .selectAs(Account::getName, TransactionVO::getAccountName)
            .leftJoin(Category.class, Category::getId, TransactionRecord::getCategoryId)
            .leftJoin(Account.class, Account::getId, TransactionRecord::getAccountId);
        if (params != null) {
            wrapper.eq(StringUtils.hasText(params.getType()), TransactionRecord::getType, params.getType())
                .eq(params.getCategoryId() != null, TransactionRecord::getCategoryId, params.getCategoryId())
                .eq(params.getAccountId() != null, TransactionRecord::getAccountId, params.getAccountId())
                .ge(params.getStartDate() != null, TransactionRecord::getTransactionDate, params.getStartDate())
                .le(params.getEndDate() != null, TransactionRecord::getTransactionDate, params.getEndDate())
                .and(StringUtils.hasText(params.getKeyword()), item -> item.like(TransactionRecord::getMerchant, params.getKeyword())
                    .or().like(TransactionRecord::getNote, params.getKeyword()));
        }
        wrapper.orderByDesc(TransactionRecord::getTransactionDate).orderByDesc(TransactionRecord::getCreatedTime);
        IPage<TransactionVO> page = baseMapper.selectJoinPage(query.toPage(), TransactionVO.class, wrapper);
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(TransactionSaveDTO dto) {
        TransactionType type = TransactionType.of(dto.getType());
        Account account = accountMapper.selectById(dto.getAccountId());
        if (account == null) {
            throw new BizException(404, "账户不存在");
        }
        if (type == TransactionType.TRANSFER) {
            return createTransfer(dto, account);
        }
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null || !category.getType().equals(dto.getType())) {
            throw new BizException(400, "请选择与收支类型一致的分类");
        }
        TransactionRecord record = new TransactionRecord();
        record.setType(dto.getType());
        record.setAmount(dto.getAmount());
        record.setCategoryId(dto.getCategoryId());
        record.setAccountId(dto.getAccountId());
        record.setTargetAccountId(null);
        record.setTransactionDate(dto.getTransactionDate());
        record.setMerchant(dto.getMerchant());
        record.setNote(dto.getNote());
        record.setSource("MANUAL");
        save(record);
        adjustBalance(account, type, dto.getAmount());
        return record.getId().toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> batchCreate(List<TransactionSaveDTO> rows, String source) {
        if (rows == null || rows.isEmpty()) {
            throw new BizException(400, "没有可导入的账目");
        }
        Set<Long> accountIds = rows.stream().map(TransactionSaveDTO::getAccountId).collect(Collectors.toSet());
        Set<Long> categoryIds = rows.stream().map(TransactionSaveDTO::getCategoryId).collect(Collectors.toSet());
        Map<Long, Account> accounts = accountMapper.selectByIds(accountIds).stream()
            .collect(Collectors.toMap(Account::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, Category> categories = categoryMapper.selectByIds(categoryIds).stream()
            .collect(Collectors.toMap(Category::getId, Function.identity()));
        if (accounts.size() != accountIds.size() || categories.size() != categoryIds.size()) {
            throw new BizException(404, "导入数据中的账户或分类不存在");
        }
        List<TransactionRecord> records = new ArrayList<>(rows.size());
        rows.forEach(dto -> {
            Category category = categories.get(dto.getCategoryId());
            if (!category.getType().equals(dto.getType())) {
                throw new BizException(400, "分类“" + category.getName() + "”与收支类型不一致");
            }
            TransactionRecord record = new TransactionRecord();
            record.setType(dto.getType());
            record.setAmount(dto.getAmount());
            record.setCategoryId(dto.getCategoryId());
            record.setAccountId(dto.getAccountId());
            record.setTransactionDate(dto.getTransactionDate());
            record.setMerchant(dto.getMerchant());
            record.setNote(dto.getNote());
            record.setSource(source);
            records.add(record);
            adjustBalance(accounts.get(dto.getAccountId()), TransactionType.of(dto.getType()), dto.getAmount());
        });
        saveBatch(records);
        accountService.updateBatchById(accounts.values());
        return records.stream().map(record -> record.getId().toString()).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String delete(Long id) {
        TransactionRecord record = getById(id);
        if (record == null) {
            throw new BizException(404, "收支记录不存在");
        }
        Account account = accountMapper.selectById(record.getAccountId());
        if (TransactionType.TRANSFER.name().equals(record.getType())) {
            reverseTransfer(record, account);
        } else if (account != null) {
            TransactionType reverse = TransactionType.INCOME.name().equals(record.getType())
                ? TransactionType.EXPENSE : TransactionType.INCOME;
            adjustBalance(account, reverse, record.getAmount());
        }
        removeById(id);
        return id.toString();
    }

    private void adjustBalance(Account account, TransactionType type, BigDecimal amount) {
        boolean liability = account.getNature() == AccountNature.LIABILITY;
        boolean increase = type == TransactionType.INCOME && !liability || type == TransactionType.EXPENSE && liability;
        account.setBalance(increase ? account.getBalance().add(amount) : account.getBalance().subtract(amount));
        accountMapper.updateById(account);
    }

    private String createTransfer(TransactionSaveDTO dto, Account sourceAccount) {
        if (dto.getTargetAccountId() == null || dto.getTargetAccountId().equals(dto.getAccountId())) {
            throw new BizException(400, "请选择不同的转入账户");
        }
        Account targetAccount = accountMapper.selectById(dto.getTargetAccountId());
        if (targetAccount == null) {
            throw new BizException(404, "转入账户不存在");
        }
        TransactionRecord record = new TransactionRecord();
        record.setType(TransactionType.TRANSFER.name());
        record.setAmount(dto.getAmount());
        record.setAccountId(dto.getAccountId());
        record.setTargetAccountId(dto.getTargetAccountId());
        record.setTransactionDate(dto.getTransactionDate());
        record.setMerchant(dto.getMerchant());
        record.setNote(dto.getNote());
        record.setSource("MANUAL");
        save(record);
        applyOutgoing(sourceAccount, dto.getAmount());
        applyIncoming(targetAccount, dto.getAmount());
        return record.getId().toString();
    }

    private void reverseTransfer(TransactionRecord record, Account sourceAccount) {
        Account targetAccount = accountMapper.selectById(record.getTargetAccountId());
        if (sourceAccount != null) {
            applyIncoming(sourceAccount, record.getAmount());
        }
        if (targetAccount != null) {
            applyOutgoing(targetAccount, record.getAmount());
        }
    }

    private void applyOutgoing(Account account, BigDecimal amount) {
        boolean liability = account.getNature() == AccountNature.LIABILITY;
        account.setBalance(liability ? account.getBalance().add(amount) : account.getBalance().subtract(amount));
        accountMapper.updateById(account);
    }

    private void applyIncoming(Account account, BigDecimal amount) {
        boolean liability = account.getNature() == AccountNature.LIABILITY;
        account.setBalance(liability ? account.getBalance().subtract(amount) : account.getBalance().add(amount));
        accountMapper.updateById(account);
    }
}
