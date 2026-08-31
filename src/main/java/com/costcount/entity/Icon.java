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
@TableName("cc_icon")
public class Icon extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图标名称
     */
    private String iconName;

    /**
     * 图标相对路径
     * 例如：
     * account-types/花呗.png
     * bookkeeping/餐饮.png
     */
    private String iconPath;

    /**
     * 图标分类
     * ACCOUNT
     * BOOKKEEPING
     */
    private String iconCategory;

    private Integer isDefault;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态：0停用，1启用
     */
    private Integer status;

    /**
     * 逻辑删除：0否，1是
     */
    @TableLogic
    private Integer isDeleted;
}