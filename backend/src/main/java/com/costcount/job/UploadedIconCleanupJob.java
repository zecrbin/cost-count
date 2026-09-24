package com.costcount.job;

import com.costcount.service.UploadedIconService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 应用启动完成后清理一次未被使用的上传图标，例如上传后点了取消、没有保存的图片。
 *
 * <p>本地记账软件不常驻运行，按启动触发比定时任务更可靠。可通过
 * {@code app.storage.uploaded-icon-cleanup.enabled=false} 关闭。</p>
 */
@Slf4j
@Component
public class UploadedIconCleanupJob {

    @Resource
    private UploadedIconService uploadedIconService;

    @Value("${app.storage.uploaded-icon-cleanup.enabled:true}")
    private boolean enabled;

    /** 上传后至少保留的时长，保护刚上传、尚未保存的图片。 */
    @Value("${app.storage.uploaded-icon-cleanup.retention:24h}")
    private Duration retention;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOnStartup() {
        if (!enabled) {
            return;
        }
        try {
            uploadedIconService.cleanupOrphanedIcons(retention);
        } catch (RuntimeException exception) {
            // 清理失败不影响正常使用，下次启动会再试。
            log.warn("启动时清理上传图标失败", exception);
        }
    }
}
