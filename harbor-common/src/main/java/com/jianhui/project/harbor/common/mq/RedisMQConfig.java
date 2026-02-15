package com.jianhui.project.harbor.common.mq;

import com.alibaba.fastjson2.support.spring6.data.redis.GenericFastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisMQConfig {

    @Bean
    public RedisMQTemplate redisMQTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisMQTemplate redisMQTemplate = new RedisMQTemplate();
        redisMQTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置值（value）的序列化采用GenericFastJsonRedisSerializer
        redisMQTemplate.setValueSerializer(fastJsonRedisSerializer());
        redisMQTemplate.setHashValueSerializer(fastJsonRedisSerializer());
        // 设置键（key）的序列化采用StringRedisSerializer。
        redisMQTemplate.setKeySerializer(new StringRedisSerializer());
        redisMQTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisMQTemplate.afterPropertiesSet();
        return redisMQTemplate;
    }

    @Bean
    public GenericFastJsonRedisSerializer fastJsonRedisSerializer() {
        return new GenericFastJsonRedisSerializer();
    }

}
