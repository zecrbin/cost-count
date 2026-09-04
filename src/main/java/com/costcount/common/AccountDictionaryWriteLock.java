package com.costcount.common;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 账户提供方和账户类型写操作共享的 JVM 锁。
 *
 * <p>当前应用按单实例部署时，用同一把锁协调两个字典服务的并发写入。
 * 多实例部署时必须替换为 Redis 或数据库分布式锁，否则不同实例之间无法互斥。</p>
 */
public final class AccountDictionaryWriteLock {

    private static final ReentrantLock LOCK = new ReentrantLock();

    private AccountDictionaryWriteLock() {
    }

    /** 获取账户字典写锁；调用方必须在 finally 中调用 {@link #release(ReentrantLock)}。 */
    public static ReentrantLock acquire() {
        LOCK.lock();
        return LOCK;
    }

    /** 在事务完成后释放锁；没有事务时立即释放。 */
    public static void release(ReentrantLock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            lock.unlock();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                lock.unlock();
            }
        });
    }
}
