package com.costcount.service;

import com.costcount.entity.Account;

public interface AccountIconService {

    /**
     * 根据提供方默认Logo + 卡尾号生成银行卡账户图标。
     *
     * @param accountTypeName 账户类型名称
     * @param providerIcon    provider图标，例如 providers/cmb.png
     * @param account         账户实体
     * @return 动态图标URL
     */
    String generateBankCardIcon(
            String accountTypeName,
            String providerIcon,
            Account account
    );

    /**
     * 按账户中已保存的图标URL删除动态账户图标。
     *
     * @param iconUrl 动态图标URL
     */
    void deleteAccountIcon(String iconUrl);
}
