package com.kidsanim.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "kidsPipelineExecutor")
    public Executor kidsPipelineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1); // Concurrencia estricta 1 para proteger la VRAM de la GPU
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("kids-pipe-");
        executor.initialize();
        return executor;
    }
}
