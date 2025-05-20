package com.jianhui.project.harbor.client.task;

import cn.hutool.core.util.StrUtil;
import com.jianhui.project.harbor.common.mq.RedisMQConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * 抽象类
 * 监听获取消息发送结果队列，然后广播给platform的listener
 */
@Slf4j
public abstract class AbstractMsgResultPullTask<T> extends RedisMQConsumer<T> {

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public String generateKey() {
        return StrUtil.join(":", super.generateKey(), appName);
    }

}
