package com.costcount.vo.account.provider;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class AccountProviderVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String providerName;

    private String icon;
}