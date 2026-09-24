package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.costcount.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 记账分类实体；父子关系由 categoryType、pid 和 Service 逻辑校验维护。 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("cc_category")
public class Category extends BaseEntity {

    /** 分类主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 父分类 ID，0 表示根分类。 */
    private Long pid;
    /** INCOME、EXPENSE 或 TRANSFER。 */
    private String categoryType;
    /** 分类展示名称。 */
    private String categoryName;
    /** 分类图标路径或图标标识；更新时允许显式清空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String icon;
    /** 同层级展示顺序。 */
    private Integer sort;
    /** 逻辑删除标识：0 未删除，1 已删除。 */
    @TableLogic
    private Integer isDeleted;
}
