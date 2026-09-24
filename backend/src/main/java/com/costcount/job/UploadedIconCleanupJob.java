package com.costcount.job;

import com.costcount.service.UploadedIconService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 定期清理未被使用的上传图标，例如上传后点了取消、没有保存的图片。
 *
 * <p>执行时间由 {@code app.storage.uploaded-icon-cleanup.cron} 配置，设为 {@code -} 可关闭。</p>
 */
@Slf4j
@Component
public class UploadedIconCleanupJob {

    @Resource
    private UploadedIconService uploadedIconService;

    /** 上传后至少保留的时长，保护仍在编辑中、尚未保存的图片。 */
    @Value("${app.storage.uploaded-icon-cleanup.retention:24h}")
    private Duration retention;

    @Scheduled(cron = "${app.storage.uploaded-icon-cleanup.cron:0 30 3 * * *}")
    public void cleanup() {
        try {
            uploadedIconService.cleanupOrphanedIcons(retention);
        } catch (RuntimeException exception) {
            log.warn("定期清理上传图标失败", exception);
        }
    }
}
