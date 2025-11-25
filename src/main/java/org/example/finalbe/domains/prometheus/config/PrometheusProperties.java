package org.example.finalbe.domains.prometheus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "prometheus")
@Getter
@Setter
public class PrometheusProperties {
    private String baseUrl;
    private Duration timeout = Duration.ofSeconds(30);
    private Collection collection = new Collection();
    private Client client = new Client();

    @Getter
    @Setter
    public static class Collection {
        private boolean enabled = true;
        private long interval = 5000;
    }

    @Getter
    @Setter
    public static class Client {
        private int connectTimeout = 10000;
        private int readTimeout = 30000;
    }
}