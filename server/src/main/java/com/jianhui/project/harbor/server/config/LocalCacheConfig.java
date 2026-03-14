package com.jianhui.project.harbor.server.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LocalCacheConfig {

    @Bean
    public Cache<String, Boolean> privateMessageDeliveryCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000_000)
                .expireAfterWrite(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public Cache<String, Boolean> groupMessageDeliveryCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000_000)
                .expireAfterWrite(Duration.ofSeconds(60))
                .build();
    }
}
