package com.costcount.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "首页财务概览")
public record DashboardVO(
    @Schema(description = "资产账户余额合计", example = "73541.15") BigDecimal assetBalance,
    @Schema(description = "负债账户待还合计", example = "12600.00") BigDecimal liabilityBalance,
    @Schema(description = "净资产，资产减去负债", example = "60941.15") BigDecimal netAssets,
    @Schema(description = "本月收入", example = "21860.00") BigDecimal monthIncome,
    @Schema(description = "本月支出", example = "7632.58") BigDecimal monthExpense,
    @Schema(description = "本月结余", example = "14227.42") BigDecimal monthBalance,
    @Schema(description = "本月记录数", example = "18") long transactionCount,
    @Schema(description = "分类支出统计") List<CategoryStatVO> expenseByCategory,
    @Schema(description = "最近收支记录") List<TransactionVO> recentTransactions
) {
}
