package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.service.IconService;
import com.costcount.vo.account.icon.IconVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "图标管理")
@RestController
@RequestMapping("/api/icons")
public class IconController {

    @Resource
    private IconService iconService;

    @Operation(summary = "查询默认账户图标")
    @GetMapping("/defaults")
    public R<List<IconVo>> listDefaults() {
        return R.ok(List.copyOf(iconService.listDefaultIconsMap().values()));
    }
}
