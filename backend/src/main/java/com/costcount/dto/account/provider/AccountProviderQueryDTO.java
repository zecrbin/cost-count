package com.costcount.dto.account.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "账户提供方查询参数")
public class AccountProviderQueryDTO {

    @Schema(description = "账户提供方名称，支持模糊查询", example = "招商")
    private String providerName;
}
