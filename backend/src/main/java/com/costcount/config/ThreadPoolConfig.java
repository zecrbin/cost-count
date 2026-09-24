package com.costcount.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration(proxyBeanMethods = false)
public class ThreadPoolConfig {

    private static final int AVAILABLE_PROCESSORS = Math.max(1, Runtime.getRuntime().availableProcessors());
    private static final int CPU_QUEUE_CAPACITY_PER_PROCESSOR = 50;
    private static final int IO_CORE_THREADS_PER_PROCESSOR = 2;
    private static final int IO_MAX_THREADS_PER_PROCESSOR = 6;
    private static final int IO_QUEUE_CAPACITY_PER_PROCESSOR = 125;

    @Bean(name = "cpuTaskExecutor")
    public ThreadPoolTaskExecutor cpuTaskExecutor() {
        return createExecutor(
                AVAILABLE_PROCESSORS,
                AVAILABLE_PROCESSORS,
                AVAILABLE_PROCESSORS * CPU_QUEUE_CAPACITY_PER_PROCESSOR,
                "cost-cpu-"
        );
    }

    @Bean(name = {"taskExecutor", "ioTaskExecutor"})
    public ThreadPoolTaskExecutor ioTaskExecutor() {
        return createExecutor(
                AVAILABLE_PROCESSORS * IO_CORE_THREADS_PER_PROCESSOR,
                AVAILABLE_PROCESSORS * IO_MAX_THREADS_PER_PROCESSOR,
                AVAILABLE_PROCESSORS * IO_QUEUE_CAPACITY_PER_PROCESSOR,
                "cost-io-"
        );
    }

    private ThreadPoolTaskExecutor createExecutor(int core, int max, int queue, String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix(prefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
