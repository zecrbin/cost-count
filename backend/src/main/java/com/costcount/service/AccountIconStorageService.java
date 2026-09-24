package com.costcount.service;

/** 账户图标文件存储服务。 */
public interface AccountIconStorageService {

    /**
     * 以提供方图标为底图、叠加账户尾号生成账户专属图标，返回可持久化的相对路径。
     *
     * <p>提供方图标缺失或无法读取时，退回按提供方名称配色绘制的卡面。</p>
     *
     * @param providerIcon 提供方图标路径，相对于 {@code /icons/default/}
     */
    String storeAccountIcon(Long accountId, String providerIcon, String providerName, String accTailNum);

    /** 删除由系统生成的账户图标，非账户图标路径将被忽略。 */
    void deleteAccountIcon(String iconPath);
}
