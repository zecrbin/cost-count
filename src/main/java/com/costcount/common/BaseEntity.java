package com.costcount.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/*
 * 通用审计字段基类。
 *
 * <p>创建/更新人和时间由 MyBatis-Plus 自动填充；逻辑删除字段由各实体按表语义自行声明。</p>
 */
public abstract class BaseEntity {

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    private String remarks;
}
