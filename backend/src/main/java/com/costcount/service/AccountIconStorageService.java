package com.costcount.service;

/** 账户动态图标文件存储服务。 */
public interface AccountIconStorageService {

    /** 生成银行卡样式图标并返回可持久化的相对路径。 */
    String storeBankCardIcon(Long accountId, String providerName, String accTailNum);

    /** 判断图标路径是否为系统生成的账户图标。 */
    boolean isGeneratedIcon(String iconPath);

    /** 删除由系统生成的账户图标，非账户图标路径将被忽略。 */
    void deleteBankCardIcon(String iconPath);
}
