package com.costcount.vo.account.provider;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "账户提供方信息")
public class AccountProviderVO {

    @Schema(description = "账户提供方ID", example = "10001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "账户提供方名称", example = "招商银行")
    private String providerName;

    @Schema(description = "图标路径", example = "providers/cmb.png")
    private String icon;
}
