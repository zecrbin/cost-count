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

    /** 账户提供方主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 账户提供方名称，例如银行、支付平台。 */
    private String providerName;

    /** 提供方图标地址；传空值时允许清空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String icon;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    @TableLogic
    private Integer isDeleted;
}
