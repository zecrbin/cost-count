package com.costcount.service;

import com.costcount.entity.Icon;
import com.costcount.vo.account.icon.IconVo;
import com.github.yulichang.base.MPJBaseService;

import java.util.Map;

/**
 * 系统默认图标查询服务。
 */
public interface IconService extends MPJBaseService<Icon> {

    /** 按图标名称返回可用的默认账户图标。 */
    Map<String, IconVo> listDefaultIconsMap();
}
