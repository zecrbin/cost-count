package com.costcount.dto.account.provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountProviderSaveDTO {

    @NotBlank(message = "账户提供方名称不能为空")
    @Size(max = 64, message = "账户提供方名称不能超过64个字符")
    private String providerName;

    private String icon;
}