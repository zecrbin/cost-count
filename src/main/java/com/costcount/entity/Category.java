package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {
    private Long parentId;
    private String name;
    private String type;
    private String icon;
    private String color;
    private Boolean systemCategory;
    private Integer sort;
}
