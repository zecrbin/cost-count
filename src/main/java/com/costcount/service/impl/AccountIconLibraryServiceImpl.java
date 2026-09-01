package com.costcount.service.impl;

import com.costcount.entity.AccountIcon;
import com.costcount.mapper.AccountIconMapper;
import com.costcount.service.AccountIconLibraryService;
import com.costcount.vo.account.icon.AccountIconVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.costcount.common.CommonConstant.IS_DEFAULT_CODE;
import static com.costcount.common.CommonConstant.NORMAL_STATUS;

@Service
public class AccountIconLibraryServiceImpl
        extends MPJBaseServiceImpl<AccountIconMapper, AccountIcon>
        implements AccountIconLibraryService {

    @Override
    public List<AccountIconVO> listDefaultAccountIcons() {
        MPJLambdaWrapper<AccountIcon> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAsClass(AccountIcon.class, AccountIconVO.class)
                .eq(AccountIcon::getIsDefault, IS_DEFAULT_CODE)
                .eq(AccountIcon::getStatus, NORMAL_STATUS)
                .orderByAsc(AccountIcon::getSort)
                .orderByDesc(AccountIcon::getCreatedTime);

        return selectJoinList(AccountIconVO.class, wrapper);
    }
}
