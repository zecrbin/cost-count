package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.costcount.enums.AccountNature;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("account")
public class Account extends BaseEntity {
    private String name;
    // 兼容旧表的非空列，新业务只以 name 和 nature 为准。
    private String type;
    private AccountNature nature;
    private BigDecimal balance;
    private BigDecimal initialBalance;
    private String color;
    private Integer sort;
}
