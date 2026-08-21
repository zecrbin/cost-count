package com.costcount.service;

import com.costcount.dto.CategorySaveDTO;
import com.costcount.entity.Category;
import com.costcount.vo.CategoryVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

public interface CategoryService extends MPJBaseService<Category> {
    List<CategoryVO> listAll();
    String create(CategorySaveDTO dto);
    String update(Long id, CategorySaveDTO dto);
}
