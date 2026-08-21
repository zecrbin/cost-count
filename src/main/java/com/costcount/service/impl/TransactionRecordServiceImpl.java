package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.costcount.dto.TransactionPageQuery;
import com.costcount.dto.TransactionQueryDTO;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.Category;
import com.costcount.entity.TransactionRecord;
import com.costcount.enums.TransactionType;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.CategoryMapper;
import com.costcount.mapper.TransactionRecordMapper;
import com.costcount.service.TransactionRecordService;
import com.costcount.vo.PageResult;
import com.costcount.vo.TransactionVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class TransactionRecordServiceImpl extends MPJBaseServiceImpl<TransactionRecordMapper, TransactionRecord> implements TransactionRecordService {
    @Resource
    private AccountMapper accountMapper;
    @Resource
    private CategoryMapper categoryMapper;

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
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (account == null || category == null) {
            throw new BizException(404, "账户或分类不存在");
        }
        if (!category.getType().equals(dto.getType())) {
            throw new BizException(400, "所选分类与收支类型不一致");
        }
        TransactionRecord record = new TransactionRecord();
        record.setType(dto.getType());
        record.setAmount(dto.getAmount());
        record.setCategoryId(dto.getCategoryId());
        record.setAccountId(dto.getAccountId());
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
    public String delete(Long id) {
        TransactionRecord record = getById(id);
        if (record == null) {
            throw new BizException(404, "收支记录不存在");
        }
        Account account = accountMapper.selectById(record.getAccountId());
        if (account != null) {
            TransactionType reverse = TransactionType.INCOME.name().equals(record.getType()) ? TransactionType.EXPENSE : TransactionType.INCOME;
            adjustBalance(account, reverse, record.getAmount());
        }
        removeById(id);
        return id.toString();
    }

    private void adjustBalance(Account account, TransactionType type, BigDecimal amount) {
        BigDecimal next = type == TransactionType.INCOME ? account.getBalance().add(amount) : account.getBalance().subtract(amount);
        account.setBalance(next);
        accountMapper.updateById(account);
    }
}
