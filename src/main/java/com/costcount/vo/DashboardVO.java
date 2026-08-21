package com.costcount.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "首页财务概览")
public record DashboardVO(
    @Schema(description = "全部账户余额", example = "73541.15") BigDecimal totalBalance,
    @Schema(description = "本月收入", example = "21860.00") BigDecimal monthIncome,
    @Schema(description = "本月支出", example = "7632.58") BigDecimal monthExpense,
    @Schema(description = "本月结余", example = "14227.42") BigDecimal monthBalance,
    @Schema(description = "本月记录数", example = "18") long transactionCount,
    @Schema(description = "分类支出统计") List<CategoryStatVO> expenseByCategory,
    @Schema(description = "最近收支记录") List<TransactionVO> recentTransactions
) {
}
