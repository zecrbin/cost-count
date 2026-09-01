package com.costcount.service;

import com.costcount.entity.AccountIcon;
import com.costcount.vo.account.icon.AccountIconVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

/**
 * 账户图标库查询服务。
 */
public interface AccountIconLibraryService extends MPJBaseService<AccountIcon> {

    List<AccountIconVO> listDefaultAccountIcons();
}
