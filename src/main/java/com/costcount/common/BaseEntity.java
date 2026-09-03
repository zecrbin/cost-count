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

    /** 记录创建时间，由 MyBatis-Plus 新增时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 创建人标识，由 MyBatis-Plus 新增时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    /** 最后更新时间，由 MyBatis-Plus 新增和更新时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /** 最后更新人标识，由 MyBatis-Plus 新增和更新时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    /** 业务备注。 */
    private String remark;
}
