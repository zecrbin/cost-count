package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.category.CategoryQueryDTO;
import com.costcount.dto.category.CategorySaveDTO;
import com.costcount.service.CategoryService;
import com.costcount.vo.category.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "收支分类管理")
@Validated
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @Operation(summary = "查询分类树")
    @PostMapping("/list")
    public R<List<CategoryVO>> list(@RequestBody CategoryQueryDTO query) {
        return R.ok(categoryService.listCategoryTree(query));
    }

    @Operation(summary = "查询分类详情")
    @GetMapping("/{id}")
    public R<CategoryVO> get(@PathVariable Long id) {
        return R.ok(categoryService.getCategory(id));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public R<String> create(@RequestBody @Valid CategorySaveDTO dto) {
        return R.ok(categoryService.addCategory(dto));
    }

    @Operation(summary = "修改分类")
    @PutMapping
    public R<String> update(@RequestBody @Valid CategorySaveDTO dto) {
        return R.ok(categoryService.updateCategory(dto));
    }

    @Operation(summary = "批量删除分类")
    @DeleteMapping
    public R<Void> delete(
            @RequestBody
            @NotEmpty(message = "分类 ID 不能为空")
            List<@NotNull(message = "分类 ID 不能为空") Long> ids
    ) {
        categoryService.deleteCategories(ids);
        return R.ok();
    }
}
