package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.costcount.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("cc_account_provider")
public class AccountProvider extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String providerName;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String icon;

    @TableLogic
    private Integer isDeleted;
}
