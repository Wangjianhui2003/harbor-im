package com.jianhui.project.harbor.server.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jianhui.project.harbor.server.config.props.GroupMessageMQProperties;
import com.jianhui.project.harbor.server.model.GroupMessageSendDedupRecord;
import com.jianhui.project.harbor.server.config.props.PrivateMessageMQProperties;
import com.jianhui.project.harbor.server.model.PrivateMessageSendDedupRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LocalCacheConfig {

    @Bean
    public Cache<String, PrivateMessageSendDedupRecord> privateMessageSendDedupCache(PrivateMessageMQProperties mqProperties) {
        return Caffeine.newBuilder()
                .maximumSize(500_000)
                .expireAfterWrite(Duration.ofSeconds(mqProperties.getDedup().getSendExpireSeconds()))
                .build();
    }

    @Bean
    public Cache<String, Boolean> privateMessageDeliveryCache(PrivateMessageMQProperties mqProperties) {
        return Caffeine.newBuilder()
                .maximumSize(1_000_000)
                .expireAfterWrite(Duration.ofSeconds(mqProperties.getDedup().getDeliveryExpireSeconds()))
                .build();
    }

    @Bean
    public Cache<String, GroupMessageSendDedupRecord> groupMessageSendDedupCache(GroupMessageMQProperties mqProperties) {
        return Caffeine.newBuilder()
                .maximumSize(500_000)
                .expireAfterWrite(Duration.ofSeconds(mqProperties.getDedup().getSendExpireSeconds()))
                .build();
    }

    @Bean
    public Cache<String, Boolean> groupMessageDeliveryCache(GroupMessageMQProperties mqProperties) {
        return Caffeine.newBuilder()
                .maximumSize(1_000_000)
                .expireAfterWrite(Duration.ofSeconds(mqProperties.getDedup().getDeliveryExpireSeconds()))
                .build();
    }
}
