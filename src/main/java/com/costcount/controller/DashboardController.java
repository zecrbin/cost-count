package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.service.DashboardService;
import com.costcount.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "财务总览")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Resource
    private DashboardService dashboardService;

    @Operation(summary = "查询本月财务概览")
    @GetMapping("/summary")
    public R<DashboardVO> summary() { return R.ok(dashboardService.getSummary()); }
}
