package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.category.CategoryQueryDTO;
import com.costcount.service.CategoryService;
import com.costcount.vo.category.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "收支分类管理")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @Operation(summary = "查询收支分类列表", description = "一级和二级分类平铺返回，按 pid 组装成树")
    @PostMapping("/list")
    public R<List<CategoryVO>> list(@RequestBody(required = false) CategoryQueryDTO query) {
        return R.ok(categoryService.listCategories(query));
    }
}
