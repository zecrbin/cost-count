package com.costcount.service.impl;

import com.costcount.config.properties.IconStorageProperties;
import com.costcount.entity.Icon;
import com.costcount.mapper.IconMapper;
import com.costcount.service.IconService;
import com.costcount.vo.account.icon.IconVo;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.costcount.common.CommonConstant.*;

@Service
public class IconServiceImpl
        extends MPJBaseServiceImpl<IconMapper, Icon>
        implements IconService {

    @Resource
    private IconStorageProperties iconStorageProperties;

    @Override
    public Map<String, IconVo> listDefaultIconsMap() {

        MPJLambdaWrapper<Icon> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAsClass(Icon.class, IconVo.class)
                .eq(Icon::getIconCategory, ICON_ACCOUNT)
                .eq(Icon::getIsDefault, IS_DEFAULT_CODE)
                .orderByAsc(Icon::getSort)
                .orderByDesc(Icon::getCreatedTime);

        return selectJoinList(IconVo.class, wrapper)
                .stream()
                .collect(Collectors.toMap(
                        IconVo::getIconName,
                        Function.identity()
                ));
    }
}