package com.costcount.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** 将运行时生成的银行卡图标和用户上传的图标映射为静态访问资源。 */
@Configuration
public class IconWebConfig implements WebMvcConfigurer {

    /** 系统生成的图标在本机文件系统中的存放目录。 */
    @Value("${app.storage.account-icon-dir:data/account-icons}")
    private String accountIconDir;

    /** 用户上传的图标在本机文件系统中的存放目录。 */
    @Value("${app.storage.uploaded-icon-dir:data/uploaded-icons}")
    private String uploadedIconDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 仅暴露这两个子路径，内置图标仍由 classpath 静态资源处理。
        registry.addResourceHandler("/icons/accounts/**").addResourceLocations(toLocation(accountIconDir));
        registry.addResourceHandler("/icons/uploads/**").addResourceLocations(toLocation(uploadedIconDir));
    }

    private String toLocation(String directory) {
        return Path.of(directory).toAbsolutePath().normalize().toUri().toString();
    }
}
