package com.costcount.service;

import com.costcount.dto.category.CategoryQueryDTO;
import com.costcount.dto.category.CategorySaveDTO;
import com.costcount.entity.Category;
import com.costcount.vo.category.CategoryVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

/**
 * 收支分类服务。
 *
 * <p>分类最多两级：一级分类的父 ID 为 0，二级分类必须挂在同类型的一级分类下。</p>
 */
public interface CategoryService extends MPJBaseService<Category> {

    /** 按类型查询分类树，一级分类下带出子分类。 */
    List<CategoryVO> listCategoryTree(CategoryQueryDTO query);

    /** 查询分类详情，不含子分类。 */
    CategoryVO getCategory(Long id);

    /** 新增分类并返回 ID。 */
    String addCategory(CategorySaveDTO dto);

    /** 修改分类并返回 ID。 */
    String updateCategory(CategorySaveDTO dto);

    /** 批量删除没有子分类且未被流水使用的分类。 */
    void deleteCategories(List<Long> categoryIds);
}
