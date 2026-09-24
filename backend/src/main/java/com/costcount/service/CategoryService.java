package com.costcount.service;

import com.costcount.dto.category.CategoryQueryDTO;
import com.costcount.entity.Category;
import com.costcount.vo.category.CategoryVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

/**
 * 收支分类服务。
 */
public interface CategoryService extends MPJBaseService<Category> {

    /** 查询分类，一级和二级平铺返回，按类型、层级、排序值排列，由调用方按 pid 组装成树。 */
    List<CategoryVO> listCategories(CategoryQueryDTO query);
}
