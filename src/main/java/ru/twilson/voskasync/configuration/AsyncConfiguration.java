package ru.twilson.voskasync.configuration;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@EnableAsync
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    @Primary
    @Bean(name = "customTaskExecutor", destroyMethod = "shutdown")
    public ExecutorService threadPoolTaskExecutor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();

        return new ThreadPoolExecutor(
                corePoolSize,                      // базовый размер пула
                corePoolSize * 2,                  // максимальный размер пула
                30L,                              // время простаивания лишних потоков (сек)
                TimeUnit.SECONDS,                 // единицы измерения времени
                new LinkedBlockingQueue<>(100),    // очередь задач
                new CustomThreadFactory("app-task-"), // фабрика потоков
                new ThreadPoolExecutor.CallerRunsPolicy() // политика переполнения
        );
    }

    static class CustomThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        CustomThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.setUncaughtExceptionHandler((t, e) ->
                    LoggerFactory.getLogger(t.getName()).error("Uncaught exception", e));
            return thread;
        }
    }
}
