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
@TableName("cc_account_type")
public class AccountType extends BaseEntity {

    /** 账户类型主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属账户提供方 ID。 */
    private Long providerId;

    /** 账户类型编码，例如 DEBIT、CREDIT。 */
    private String typeCode;

    /** 账户类型名称，例如储蓄卡、信用卡。 */
    private String typeName;

    /** 展示排序值，越小越靠前。 */
    private Integer sort;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    @TableLogic
    private Integer isDeleted;
}
