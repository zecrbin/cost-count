package com.costcount.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 开启定时任务，例如定期清理未被使用的上传图标。 */
@EnableScheduling
@Configuration(proxyBeanMethods = false)
public class SchedulingConfig {
}
