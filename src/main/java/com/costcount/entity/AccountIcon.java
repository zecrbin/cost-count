package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.costcount.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("cc_account_icon")
public class AccountIcon extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String iconName;

    private String iconPath;

    private Integer isDefault;

    private Integer sort;

    private Integer status;

    @TableLogic
    private Integer isDeleted;
}
