package com.costcount.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "账户保存参数")
public class AccountSaveDTO {

    @Schema(description = "账户 ID，修改时必填", example = "30001")
    private Long id;

    @Schema(description = "账户类型 ID", example = "20001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账户类型 ID 不能为空")
    private Long typeId;

    @Schema(description = "账户名称", example = "招商银行储蓄卡", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账户名称不能为空")
    @Size(max = 256, message = "账户名称不能超过256个字符")
    private String accName;

    @Schema(description = "账户尾号，如银行卡后4位", example = "1234")
    @Pattern(regexp = "^$|^\\d{4}$", message = "账户尾号必须为4位数字")
    private String accTailNum;

    @Schema(description = "初始资金；资产账户为初始余额，负债账户为初始欠款，仅新增时生效", example = "1000.00")
    private BigDecimal initialBalance;

    @Schema(description = "初始资金发生时间，仅新增时生效，默认当前时间", example = "2026-09-03 09:00:00")
    private LocalDateTime initialTransactionTime;

    @Schema(description = "当前信用额度，仅信用类账户使用", example = "50000.00")
    @DecimalMin(value = "0.00", message = "信用额度不能小于0")
    private BigDecimal creditLimit;

    @Schema(description = "理想信用额度，仅信用类账户使用", example = "30000.00")
    @DecimalMin(value = "0.00", message = "理想信用额度不能小于0")
    private BigDecimal idealCreditLimit;

    @Schema(description = "账户展示图标 URL", example = "accounts/cmb.png")
    @Size(max = 256, message = "图标路径不能超过256个字符")
    private String icon;

    @Schema(description = "显示顺序，数值越小越靠前", example = "1")
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort = 0;

    @Schema(description = "账户状态：1正常，0停用", example = "1")
    private Integer status = 1;

    @Schema(description = "账户备注", example = "工资卡")
    @Size(max = 256, message = "备注不能超过256个字符")
    private String remark;
}
