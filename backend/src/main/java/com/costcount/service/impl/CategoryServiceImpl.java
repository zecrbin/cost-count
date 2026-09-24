package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.costcount.common.AccountDictionaryWriteLock;
import com.costcount.dto.category.CategoryQueryDTO;
import com.costcount.dto.category.CategorySaveDTO;
import com.costcount.entity.Category;
import com.costcount.entity.Transaction;
import com.costcount.exception.BizException;
import com.costcount.mapper.CategoryMapper;
import com.costcount.mapper.TransactionMapper;
import com.costcount.service.CategoryService;
import com.costcount.vo.category.CategoryVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static com.costcount.common.CommonConstant.DEFAULT_SORT;
import static com.costcount.common.CommonConstant.ROOT_CATEGORY_PID;
import static com.costcount.common.TransactionConstant.EXPENSE;
import static com.costcount.common.TransactionConstant.INCOME;
import static com.costcount.common.TransactionConstant.TRANSFER;

@Service
public class CategoryServiceImpl
        extends MPJBaseServiceImpl<CategoryMapper, Category>
        implements CategoryService {

    private static final Set<String> CATEGORY_TYPES = Set.of(INCOME, EXPENSE, TRANSFER);

    @Resource
    private TransactionMapper transactionMapper;

    @Override
    public List<CategoryVO> listCategoryTree(CategoryQueryDTO query) {
        String categoryType = query == null || !StringUtils.hasText(query.getCategoryType())
                ? null
                : normalizeType(query.getCategoryType());
        List<Category> categories = lambdaQuery()
                .eq(categoryType != null, Category::getCategoryType, categoryType)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getCreatedTime)
                .list();

        Map<Long, CategoryVO> roots = new LinkedHashMap<>();
        categories.stream()
                .filter(category -> isRoot(category.getPid()))
                .forEach(category -> roots.put(category.getId(), toVO(category)));
        categories.stream()
                .filter(category -> !isRoot(category.getPid()))
                .forEach(category -> {
                    CategoryVO parent = roots.get(category.getPid());
                    if (parent != null) {
                        parent.getChildren().add(toVO(category));
                    }
                });
        return new ArrayList<>(roots.values());
    }

    @Override
    public CategoryVO getCategory(Long id) {
        Category category = getById(id);
        if (category == null) {
            throw new BizException(404, "分类不存在");
        }
        return toVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addCategory(CategorySaveDTO dto) {
        validateSaveDTO(dto);
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            Category category = new Category();
            applySaveDTO(category, dto);
            validateParentAndDuplicate(category);
            save(category);
            return String.valueOf(category.getId());
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateCategory(CategorySaveDTO dto) {
        validateSaveDTO(dto);
        if (dto.getId() == null) {
            throw new BizException(400, "分类 ID 不能为空");
        }
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            Category category = getById(dto.getId());
            if (category == null) {
                throw new BizException(404, "分类不存在");
            }
            boolean hasChildren = hasChildren(List.of(category.getId()));
            String newType = normalizeType(dto.getCategoryType());
            if (!newType.equals(category.getCategoryType())
                    && (hasChildren || isUsedByTransaction(List.of(category.getId())))) {
                throw new BizException(409, "分类已有子分类或已被流水使用，不能修改分类类型");
            }
            if (hasChildren && !ROOT_CATEGORY_PID.equals(normalizePid(dto.getPid()))) {
                throw new BizException(400, "已有子分类的分类不能移动到其他分类下");
            }
            applySaveDTO(category, dto);
            validateParentAndDuplicate(category);
            updateById(category);
            return String.valueOf(category.getId());
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty() || categoryIds.stream().anyMatch(Objects::isNull)) {
            throw new BizException(400, "分类 ID 不能为空");
        }
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            List<Long> ids = categoryIds.stream().distinct().toList();
            // 同时删除父分类和它的全部子分类是允许的，只拦截会留下孤儿子分类的情况。
            if (lambdaQuery().in(Category::getPid, ids).notIn(Category::getId, ids).exists()) {
                throw new BizException(409, "请先删除子分类");
            }
            if (isUsedByTransaction(ids)) {
                throw new BizException(409, "分类已被流水使用，无法删除");
            }
            removeByIds(ids);
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    private void applySaveDTO(Category category, CategorySaveDTO dto) {
        category.setPid(normalizePid(dto.getPid()));
        category.setCategoryType(normalizeType(dto.getCategoryType()));
        category.setCategoryName(dto.getCategoryName().trim());
        category.setIcon(normalize(dto.getIcon()));
        category.setSort(dto.getSort() == null ? DEFAULT_SORT : dto.getSort());
        category.setRemark(normalize(dto.getRemark()));
    }

    private void validateParentAndDuplicate(Category category) {
        if (!ROOT_CATEGORY_PID.equals(category.getPid())) {
            if (category.getPid().equals(category.getId())) {
                throw new BizException(400, "父分类不能是自身");
            }
            Category parent = getById(category.getPid());
            if (parent == null) {
                throw new BizException(404, "父分类不存在");
            }
            if (!isRoot(parent.getPid())) {
                throw new BizException(400, "分类最多支持两级");
            }
            if (!parent.getCategoryType().equals(category.getCategoryType())) {
                throw new BizException(400, "子分类类型必须与父分类一致");
            }
        }
        boolean root = isRoot(category.getPid());
        if (lambdaQuery().eq(Category::getCategoryType, category.getCategoryType())
                // 历史数据的一级分类可能以 NULL 作为父 ID，查重时一并视为根层级。
                .and(root, w -> w.eq(Category::getPid, ROOT_CATEGORY_PID).or().isNull(Category::getPid))
                .eq(!root, Category::getPid, category.getPid())
                .eq(Category::getCategoryName, category.getCategoryName())
                .ne(category.getId() != null, Category::getId, category.getId())
                .exists()) {
            throw new BizException(409, "同一层级下已存在相同名称的分类");
        }
    }

    private void validateSaveDTO(CategorySaveDTO dto) {
        if (dto == null) {
            throw new BizException(400, "分类参数不能为空");
        }
        if (!StringUtils.hasText(dto.getCategoryType())
                || !CATEGORY_TYPES.contains(normalizeType(dto.getCategoryType()))) {
            throw new BizException(400, "分类类型只能是 INCOME、EXPENSE 或 TRANSFER");
        }
        if (!StringUtils.hasText(dto.getCategoryName())) {
            throw new BizException(400, "分类名称不能为空");
        }
        if (dto.getSort() != null && dto.getSort() < 0) {
            throw new BizException(400, "排序值不能小于0");
        }
    }

    private boolean hasChildren(List<Long> categoryIds) {
        return lambdaQuery().in(Category::getPid, categoryIds).exists();
    }

    private boolean isUsedByTransaction(List<Long> categoryIds) {
        return transactionMapper.exists(new LambdaQueryWrapper<Transaction>()
                .in(Transaction::getCategoryId, categoryIds));
    }

    private CategoryVO toVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }

    private boolean isRoot(Long pid) {
        return pid == null || ROOT_CATEGORY_PID.equals(pid);
    }

    private Long normalizePid(Long pid) {
        return pid == null ? ROOT_CATEGORY_PID : pid;
    }

    private String normalizeType(String categoryType) {
        return categoryType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
