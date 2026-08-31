package com.costcount.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long providerId;

    private String providerName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long accTypeId;

    private String typeCode;

    private String typeName;

    private String accName;

    private String accTailNum;

    private BigDecimal balance;

    private BigDecimal creditLimit;

    private BigDecimal idealCreditLimit;

    /**
     * 信用账户可用额度
     */
    private BigDecimal availableCredit;

    private String icon;

    private Integer sort;

    private Integer status;

    private String remark;

    private LocalDateTime createdTime;
}