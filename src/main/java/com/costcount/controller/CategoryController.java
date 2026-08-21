package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.CategorySaveDTO;
import com.costcount.service.CategoryService;
import com.costcount.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "收支分类")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    @Resource
    private CategoryService categoryService;

    @Operation(summary = "查询全部收支分类")
    @GetMapping("/list")
    public R<List<CategoryVO>> list() { return R.ok(categoryService.listAll()); }

    @Operation(summary = "新增自定义分类")
    @PostMapping
    public R<String> create(@RequestBody @Valid CategorySaveDTO dto) { return R.ok(categoryService.create(dto)); }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public R<String> update(@PathVariable Long id, @RequestBody @Valid CategorySaveDTO dto) {
        return R.ok(categoryService.update(id, dto));
    }
}
