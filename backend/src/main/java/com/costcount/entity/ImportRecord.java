package com.costcount.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.costcount.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 流水导入记录实体。 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("cc_import_record")
public class ImportRecord extends BaseEntity {

    /** 导入记录主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 导入来源类型：TEMPLATE、WECHAT、BANK 或 AGENT。 */
    private String importType;

    /** 上传文件的原始名称。 */
    private String uploadFileName;

    /** 原始导入文件的存储地址。 */
    private String fileUrl;

    /** 文件 SHA-256 摘要，用于完整性与重复上传识别。 */
    private String fileHash;

    /** 文件大小，单位为字节。 */
    private Long fileSize;

    /** 导入状态：PROCESSING、SUCCESS 或 FAILED。 */
    private String status;

    /** 文件中的流水总数量。 */
    private Integer totalCount;

    /** 成功导入的流水数量。 */
    private Integer successCount;

    /** 识别为重复流水的数量。 */
    private Integer duplicateCount;

    /** 导入失败原因。 */
    private String errorMessage;

    /** 导入发生时间。 */
    private LocalDateTime importTime;
}
