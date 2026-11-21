package com.bolt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides named thread pools for ATM and Online banking channels.
 */
@Configuration
public class ExecutorConfig {

    @Bean(name = "atmExecutor", destroyMethod = "shutdown")
    public ExecutorService atmExecutor() {
        return Executors.newFixedThreadPool(4, new NamedThreadFactory("ATM-THREAD"));
    }

    @Bean(name = "onlineExecutor", destroyMethod = "shutdown")
    public ExecutorService onlineExecutor() {
        return Executors.newFixedThreadPool(4, new NamedThreadFactory("ONLINE-THREAD"));
    }

    /**
     * ThreadFactory that produces threads with identifiable names
     * such as ATM-THREAD-1, ATM-THREAD-2, etc.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
