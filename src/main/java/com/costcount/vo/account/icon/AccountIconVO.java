package com.costcount.vo.account.icon;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AccountIconVO {

    @Schema(description = "账户图标ID", example = "30001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "图标名称", example = "花呗")
    private String iconName;

    @Schema(description = "图标相对路径", example = "account-types/花呗.png")
    private String iconPath;

    @Schema(description = "排序号", example = "1")
    private Integer sort;

    @Schema(description = "状态：0停用，1启用", example = "1")
    private Integer status;
}
