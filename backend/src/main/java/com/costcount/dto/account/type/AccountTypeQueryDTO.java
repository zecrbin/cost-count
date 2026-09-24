package com.costcount.dto.account.type;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "账户类型查询参数")
public class AccountTypeQueryDTO {

    @Schema(description = "账户提供方 ID", example = "10001")
    private Long providerId;

    @Schema(description = "账户类型编码，支持模糊查询", example = "DEBIT")
    private String typeCode;

    @Schema(description = "账户类型名称，支持模糊查询", example = "储蓄卡")
    private String typeName;

}
