package com.jianhui.project.harbor.platform.config;

import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@EnableCaching
@Configuration
public class RedisConfig {

    @Resource
    private RedisConnectionFactory factory;

    @Primary
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        RedisSerializer<Object> valueSerializer = jsonRedisSerializer();
        // 使用 Spring Data Redis 4 推荐的 JSON 序列化器。
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);
        // 设置键（key）的序列化采用StringRedisSerializer。
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    @Bean
    public CacheManager cacheManager() {
        // 设置redis缓存管理器
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(600))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer()));
        return RedisCacheManager.builder(factory).cacheDefaults(cacheConfiguration).build();
    }


    @Bean
    public RedisSerializer<Object> jsonRedisSerializer() {
        return RedisSerializer.json();
    }
}
