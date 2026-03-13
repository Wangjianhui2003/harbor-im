package com.jianhui.project.harbor.platform.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LocalCacheConfig {

    @Bean
    public Cache<String, Boolean> friendLocalCache() {
        return Caffeine.newBuilder()
                .maximumSize(200_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }
}
