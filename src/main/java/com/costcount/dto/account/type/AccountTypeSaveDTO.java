package com.costcount.dto.account.type;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountTypeSaveDTO {

    @NotNull(message = "账户提供方不能为空")
    private Long accProviderId;

    @NotBlank(message = "账户类型编码不能为空")
    private String typeCode;

    @NotBlank(message = "账户类型名称不能为空")
    private String typeName;

    private String icon;

    private Integer sort = 0;

    private Integer status = 1;
}