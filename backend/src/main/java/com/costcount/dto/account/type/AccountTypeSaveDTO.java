package com.costcount.dto.account.type;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "账户类型保存参数")
public class AccountTypeSaveDTO {

    @Schema(description = "账户类型 ID，修改时必填", example = "20001")
    private Long id;

    @Schema(description = "账户提供方 ID", example = "10001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账户提供方 ID 不能为空")
    private Long providerId;

    @Schema(description = "账户类型编码，决定余额方向：DEBIT 存储账户、CREDIT 信用账户", example = "DEBIT",
            allowableValues = {"DEBIT", "CREDIT"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账户类型编码不能为空")
    @Size(max = 64, message = "账户类型编码不能超过64个字符")
    private String typeCode;

    @Schema(description = "账户类型名称", example = "储蓄卡", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账户类型名称不能为空")
    @Size(max = 128, message = "账户类型名称不能超过128个字符")
    private String typeName;

    @Schema(description = "显示顺序，数值越小越靠前", example = "1")
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort = 0;

}
