package org.example.finalbe.domains.common.config;

/**
 * packageName    : org.example.finalbe.domains.common.config
 * fileName       : AsyncConfig
 * author         : {sana}
 * date           : 25. 11. 18.
 * description    : 자동 주석 생성
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 11. 18.        {sana}       최초 생성
 */


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);     // 기본 스레드 수
        executor.setMaxPoolSize(50);      // 최대 스레드 수
        executor.setQueueCapacity(100);   // 대기 큐 크기
        executor.setThreadNamePrefix("SSE-Async-");
        executor.initialize();
        return executor;
    }
}