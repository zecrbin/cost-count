package com.costcount.service.impl;

import com.costcount.entity.Category;
import com.costcount.mapper.CategoryMapper;
import com.costcount.service.CategoryService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl
        extends MPJBaseServiceImpl<CategoryMapper, Category>
        implements CategoryService {
}
