package com.jianhui.project.harbor.client.task;

import cn.hutool.core.util.StrUtil;
import com.jianhui.project.harbor.common.mq.RedisMQConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * 抽象类
 * 用于拉取消息队列中
 */
@Slf4j
public abstract class AbstractMessageResultTask<T> extends RedisMQConsumer<T> {

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public String generateKey() {
        return StrUtil.join(":", super.generateKey(), appName);
    }

}
