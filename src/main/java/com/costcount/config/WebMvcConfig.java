package com.costcount.config;

import com.costcount.config.properties.IconStorageProperties;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final IconStorageProperties iconStorageProperties;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {

        IconStorageProperties.Generated generated =
                iconStorageProperties.getGenerated();

        Path storagePath = Path.of(generated.getPath())
                .toAbsolutePath()
                .normalize();

        String urlPrefix = generated.getUrlPrefix();

        if (!urlPrefix.endsWith("/")) {
            urlPrefix += "/";
        }

        registry
                .addResourceHandler(urlPrefix + "**")
                .addResourceLocations(storagePath.toUri().toString());
    }
}