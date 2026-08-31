package com.costcount.service;

import com.costcount.entity.Icon;
import com.costcount.vo.account.icon.IconVo;
import com.github.yulichang.base.MPJBaseService;

import java.util.Map;

public interface IconService extends MPJBaseService<Icon> {
    Map<String, IconVo> listDefaultIconsMap();
}