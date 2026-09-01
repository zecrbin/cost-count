package com.costcount.mapper;

import com.costcount.entity.Category;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 分类数据访问接口，父分类关系由业务层维护。 */
@Mapper
public interface CategoryMapper extends MPJBaseMapper<Category> {
}
