package com.costcount.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** 将运行时生成的银行卡图标映射为静态访问资源。 */
@Configuration
public class AccountIconWebConfig implements WebMvcConfigurer {

    /** 系统生成的图标在本机文件系统中的存放目录。 */
    @Value("${app.storage.account-icon-dir:data/account-icons}")
    private String accountIconDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(accountIconDir).toAbsolutePath().normalize().toUri().toString();
        // 仅暴露账户图标子路径，固定提供方图标仍由 classpath 静态资源处理。
        registry.addResourceHandler("/icons/accounts/**").addResourceLocations(location);
    }
}
