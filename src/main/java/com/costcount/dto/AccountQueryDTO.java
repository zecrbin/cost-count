package com.costcount.dto;

import lombok.Data;

@Data
public class AccountQueryDTO {

    /**
     * 提供方ID
     */
    private Long providerId;

    /**
     * DEBIT / CREDIT
     */
    private String typeCode;

    /**
     * 账户状态
     */
    private Integer status;

    /**
     * 账户名称模糊搜索
     */
    private String keyword;
}