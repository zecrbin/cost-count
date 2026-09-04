package com.costcount.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountDictionaryWriteLockTest {

    @Test
    void acquireShouldSerializeConcurrentWriters() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ReentrantLock firstLock = AccountDictionaryWriteLock.acquire();
        CountDownLatch secondWriterStarted = new CountDownLatch(1);
        try {
            Future<Boolean> secondWriter = executor.submit(() -> {
                secondWriterStarted.countDown();
                ReentrantLock lock = AccountDictionaryWriteLock.acquire();
                try {
                    return true;
                } finally {
                    AccountDictionaryWriteLock.release(lock);
                }
            });

            assertTrue(secondWriterStarted.await(1, TimeUnit.SECONDS));
            assertFalse(secondWriter.isDone());
            AccountDictionaryWriteLock.release(firstLock);
            assertTrue(secondWriter.get());
        } finally {
            if (firstLock.isHeldByCurrentThread()) {
                AccountDictionaryWriteLock.release(firstLock);
            }
            executor.shutdownNow();
        }
    }
}
