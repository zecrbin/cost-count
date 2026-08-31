package com.costcount.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage.icon")
public class IconStorageProperties {

    /**
     * 默认图标URL前缀。
     * 对应：
     * src/main/resources/static/icons/default/
     */
    private String defaultPrefix;

    /**
     * 动态生成图标配置。
     */
    private Generated generated = new Generated();

    @Data
    public static class Generated {

        /**
         * 动态生成图标实际存储目录。
         */
        private String path;

        /**
         * 动态生成图标访问URL前缀。
         */
        private String urlPrefix;
    }
}