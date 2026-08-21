package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("account")
public class Account extends BaseEntity {
    private String name;
    private String type;
    private BigDecimal balance;
    private BigDecimal initialBalance;
    private String color;
    private Integer sort;
}
