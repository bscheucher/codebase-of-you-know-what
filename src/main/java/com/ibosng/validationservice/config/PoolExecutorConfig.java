package com.ibosng.validationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
public class PoolExecutorConfig {

    /**
     * taskExecutor have always <b>CorePoolSize</b> threads </br>
     * when <b>CorePoolSize</b> accessed, thread will be in the in Queue</br>
     * when <b>QueueCapacity</b> accessed, additional threads will be created until <b>MaxPoolSize</b> will be reached</br>
     * if MaxPoolSize reached -> Exception thrown
     * */
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(150); //always will keep alive even in idle
        executor.setQueueCapacity(200);
        executor.setMaxPoolSize(200);
        executor.setThreadNamePrefix("Async task - ");
        executor.initialize();
        return executor;
    }
}
