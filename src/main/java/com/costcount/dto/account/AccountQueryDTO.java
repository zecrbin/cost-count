package com.costcount.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "账户查询参数")
public class AccountQueryDTO {

    @Schema(description = "账户类型 ID", example = "20001")
    private Long typeId;

    @Schema(description = "账户名称，支持模糊查询", example = "招商银行")
    private String accName;

    @Schema(description = "账户尾号，支持模糊查询", example = "1234")
    private String accTailNum;

    @Schema(description = "账户状态：1正常，0停用", example = "1")
    private Integer status;
}
