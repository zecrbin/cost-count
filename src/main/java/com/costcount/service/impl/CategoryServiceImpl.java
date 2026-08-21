package com.costcount.service.impl;

import com.costcount.dto.CategorySaveDTO;
import com.costcount.entity.Category;
import com.costcount.enums.TransactionType;
import com.costcount.exception.BizException;
import com.costcount.mapper.CategoryMapper;
import com.costcount.service.CategoryService;
import com.costcount.vo.CategoryVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl extends MPJBaseServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> listAll() {
        List<Category> categories = lambdaQuery().orderByAsc(Category::getType).orderByAsc(Category::getSort).list();
        return Optional.ofNullable(categories).orElseGet(List::of).stream().map(category -> {
            CategoryVO vo = new CategoryVO();
            BeanUtils.copyProperties(category, vo);
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CategorySaveDTO dto) {
        TransactionType.of(dto.getType());
        boolean exists = lambdaQuery().eq(Category::getType, dto.getType()).eq(Category::getName, dto.getName()).exists();
        if (exists) {
            throw new BizException(409, "同类型下已存在该分类");
        }
        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        category.setSystemCategory(false);
        category.setColor(dto.getColor() == null ? "#3154E5" : dto.getColor());
        category.setSort(Math.toIntExact(count() + 1));
        categoryMapper.insert(category);
        return category.getId().toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String update(Long id, CategorySaveDTO dto) {
        Category category = getById(id);
        if (category == null) {
            throw new BizException(404, "分类不存在");
        }
        TransactionType.of(dto.getType());
        boolean exists = lambdaQuery().eq(Category::getType, dto.getType()).eq(Category::getName, dto.getName())
            .ne(Category::getId, id).exists();
        if (exists) {
            throw new BizException(409, "同类型下已存在该分类");
        }
        BeanUtils.copyProperties(dto, category, "systemCategory");
        updateById(category);
        return id.toString();
    }
}
