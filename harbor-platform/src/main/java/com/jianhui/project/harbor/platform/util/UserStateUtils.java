package com.jianhui.project.harbor.platform.util;

import cn.hutool.core.util.StrUtil;
import com.jianhui.project.harbor.platform.constant.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
/**
 * 用于记录实时通话时(WebRTC)的用户状态
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStateUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public void setBusy(Long userId){
        String key = StrUtil.join(":", RedisKey.IM_USER_BUSY,userId);
        redisTemplate.opsForValue().set(key,1,30, TimeUnit.SECONDS);
    }

    public void expire(Long userId){
        String key = StrUtil.join(":", RedisKey.IM_USER_BUSY,userId);
        redisTemplate.expire(key,30, TimeUnit.SECONDS);
    }

    public void setFree(Long userId){
        String key = StrUtil.join(":", RedisKey.IM_USER_BUSY,userId);
        redisTemplate.delete(key);
    }

    public Boolean isBusy(Long userId){
        String key = StrUtil.join(":", RedisKey.IM_USER_BUSY,userId);
        return  redisTemplate.hasKey(key);
    }

}
