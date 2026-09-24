package com.costcount.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 在事务提交后执行文件清理等不可回滚的操作。
 *
 * <p>事务回滚时不执行，避免数据库仍引用的文件被提前删除；没有事务时立即执行。
 * 清理失败只记录日志，不影响已提交的业务数据。</p>
 */
@Slf4j
public final class AfterCommit {

    private AfterCommit() {
    }

    public static void run(String description, Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runQuietly(description, action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runQuietly(description, action);
            }
        });
    }

    private static void runQuietly(String description, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.warn("{}失败", description, exception);
        }
    }
}
