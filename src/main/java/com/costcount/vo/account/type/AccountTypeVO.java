package com.costcount.vo.account.type;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class AccountTypeVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long accProviderId;

    private String providerName;

    private String typeCode;

    private String typeName;

    private String icon;

    private Integer sort;

    private Integer status;
}