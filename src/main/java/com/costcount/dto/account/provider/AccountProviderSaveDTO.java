package com.costcount.dto.account.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "账户提供方保存参数")
public class AccountProviderSaveDTO {

    @Schema(description = "账户提供方ID，修改时必填", example = "10001")
    private Long id;

    @Schema(description = "账户提供方名称", example = "招商银行", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账户提供方名称不能为空")
    @Size(max = 64, message = "账户提供方名称不能超过64个字符")
    private String providerName;

    @Schema(description = "图标路径；传空值可清除图标", example = "providers/cmb.png")
    @Size(max = 255, message = "图标路径不能超过255个字符")
    private String icon;
}
