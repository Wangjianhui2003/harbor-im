package com.jianhui.project.harbor.common.mq;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Properties;

public class RedisMQTemplate extends RedisTemplate<String, Object> {

    private String version = "";

    /**
     * 获取redis版本
     */
    public String getVersion() {
        if (version.isEmpty()) {
            RedisConnection connection = RedisConnectionUtils.getConnection(getConnectionFactory());
            Properties properties = connection.info();
            version = properties.getProperty("redis_version");
            RedisConnectionUtils.releaseConnection(connection, getConnectionFactory());
        }
        return version;
    }

    /**
     * 是否支持批量拉取，redis版本大于6.2支持批量拉取
     */
    Boolean isSupportBatchPull() {
        String version = getVersion();
        String[] arr = version.split("\\.");
        if (arr.length < 2) {
            return false;
        }
        Integer firVersion = Integer.valueOf(arr[0]);
        Integer secVersion = Integer.valueOf(arr[1]);
        return firVersion > 6 || (firVersion == 6 && secVersion >= 2);
    }

}
