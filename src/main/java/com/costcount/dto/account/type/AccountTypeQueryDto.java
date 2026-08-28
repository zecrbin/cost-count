package com.costcount.dto.account.type;

import lombok.Data;

@Data
public class AccountTypeQueryDto {
    private Long accProviderId;
    private String typeCode;
    private String typeName;
    private Integer status;
}
