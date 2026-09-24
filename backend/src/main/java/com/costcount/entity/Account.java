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

import java.math.BigDecimal;

/**
 * 账户实体。
 *
 * <p>资产类账户的余额表示可用资产，信用类账户的余额表示待还金额。</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("cc_account")
public class Account extends BaseEntity {

    /** 账户主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属账户类型 ID。 */
    private Long typeId;

    /** 账户名称。 */
    private String accName;

    /** 账户尾号，例如银行卡后四位；更新时允许显式清空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String accTailNum;

    /**
     * DEBIT：当前实际余额
     * CREDIT：当前待还金额
     */
    private BigDecimal balance;

    /**
     * 信用额度，仅 CREDIT 使用
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal creditLimit;

    /**
     * 理想信用额度，仅 CREDIT 使用
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal idealCreditLimit;

    /** 账户展示图标地址；更新时允许显式清空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String icon;

    /** 展示排序值，越小越靠前。 */
    private Integer sort;

    /** 账户状态：1 正常，0 停用。 */
    private Integer status;

    /** 逻辑删除标识：0 未删除，1 已删除。 */
    @TableLogic
    private Integer isDeleted;
}
