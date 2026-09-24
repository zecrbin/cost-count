package com.costcount.service;

import org.springframework.web.multipart.MultipartFile;

/** 用户上传的机构、账户图标存储服务。 */
public interface UploadedIconService {

    /** 校验并保存上传的图片，返回可持久化的相对路径（uploads/xxx.png）。 */
    String storeUploadedIcon(MultipartFile file);

    /** 判断图标路径是否为用户上传的图标。 */
    boolean isUploadedIcon(String iconPath);

    /** 删除用户上传的图标，非上传图标路径将被忽略。 */
    void deleteUploadedIcon(String iconPath);

    /** 在事务提交后删除不再引用的上传图标；新旧路径相同或不是上传图标时忽略。 */
    void deleteReplacedIconAfterCommit(String previousIcon, String currentIcon);
}
