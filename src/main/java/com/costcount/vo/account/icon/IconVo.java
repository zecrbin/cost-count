package com.costcount.vo.account.icon;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class IconVo {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String iconName;

    private String iconPath;

    private String iconCategory;

    private Integer sort;

    private Integer status;
}
