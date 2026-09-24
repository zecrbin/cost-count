package com.costcount.service.impl;

import com.costcount.dto.category.CategoryQueryDTO;
import com.costcount.entity.Category;
import com.costcount.mapper.CategoryMapper;
import com.costcount.service.CategoryService;
import com.costcount.vo.category.CategoryVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CategoryServiceImpl
        extends MPJBaseServiceImpl<CategoryMapper, Category>
        implements CategoryService {

    @Override
    public List<CategoryVO> listCategories(CategoryQueryDTO query) {
        String categoryType = query == null ? null : query.getCategoryType();

        MPJLambdaWrapper<Category> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(
                        Category::getId,
                        Category::getPid,
                        Category::getCategoryType,
                        Category::getCategoryName,
                        Category::getIcon,
                        Category::getSort
                )
                .eq(StringUtils.hasText(categoryType), Category::getCategoryType, categoryType)
                .orderByAsc(Category::getCategoryType)
                .orderByAsc(Category::getPid)
                .orderByAsc(Category::getSort);

        return selectJoinList(CategoryVO.class, wrapper);
    }
}
