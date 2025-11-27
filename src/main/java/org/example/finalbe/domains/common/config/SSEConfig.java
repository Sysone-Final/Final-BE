/**
 * 작성자: 황요한
 * SSE 스트림 및 모니터링 API용 CORS 설정
 */
package org.example.finalbe.domains.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public class SSEConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        // Prometheus 스트림 CORS 허용
        registry.addMapping("/api/prometheus/metrics/stream/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type", "Cache-Control")
                .maxAge(3600);

        // Monitoring SSE CORS 허용
        registry.addMapping("/api/monitoring/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type", "Cache-Control", "Last-Event-ID")
                .maxAge(3600);

        registry.addMapping("/api/alerts/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type", "Cache-Control", "Last-Event-ID")
                .maxAge(3600);
    }
}
