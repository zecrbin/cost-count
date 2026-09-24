package com.costcount.vo.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "账户信息")
public class AccountVO {

    @Schema(description = "账户 ID", example = "30001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "账户类型 ID", example = "20001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long typeId;

    @Schema(description = "账户类型编码", example = "DEBIT")
    private String typeCode;

    @Schema(description = "账户类型名称", example = "储蓄卡")
    private String typeName;

    @Schema(description = "账户提供方名称", example = "招商银行")
    private String providerName;

    @Schema(description = "账户名称", example = "招商银行储蓄卡")
    private String accName;

    @Schema(description = "账户尾号", example = "1234")
    private String accTailNum;

    @Schema(description = "当前余额；资产账户为可用余额，信用账户为待还金额", example = "1000.00")
    private BigDecimal balance;

    @Schema(description = "初始资金；资产账户为初始余额，负债账户为初始欠款", example = "1000.00")
    private BigDecimal initialBalance;

    @Schema(description = "初始资金发生时间", example = "2026-09-03 09:00:00")
    private LocalDateTime initialTransactionTime;

    @Schema(description = "当前信用额度", example = "50000.00")
    private BigDecimal creditLimit;

    @Schema(description = "理想信用额度", example = "30000.00")
    private BigDecimal idealCreditLimit;

    @Schema(description = "账户展示图标 URL", example = "accounts/cmb.png")
    private String icon;

    @Schema(description = "账户提供方固定图标 URL", example = "providers/cmb.png")
    private String providerIcon;

    @Schema(description = "显示顺序", example = "1")
    private Integer sort;

    @Schema(description = "账户状态：1正常，0停用", example = "1")
    private Integer status;

    @Schema(description = "账户备注", example = "工资卡")
    private String remark;
}
