package com.luojiajun.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AsyncConfig {

    @Bean("chatExecutor")
    public Executor chatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("chat-stream-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean("heartbeatExecutor")
    public ScheduledExecutorService heartbeatExecutor() {
        AtomicInteger index = new AtomicInteger();
        return Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "sse-heartbeat-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
