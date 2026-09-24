package com.costcount.vo.account.type;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "账户类型信息")
public class AccountTypeVO {

    @Schema(description = "账户类型 ID", example = "20001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "账户提供方 ID", example = "10001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long providerId;

    @Schema(description = "账户提供方名称", example = "中国银行")
    private String providerName;

    @Schema(description = "账户类型编码", example = "DEBIT")
    private String typeCode;

    @Schema(description = "账户类型名称", example = "储蓄卡")
    private String typeName;

    @Schema(description = "显示顺序", example = "1")
    private Integer sort;

}
