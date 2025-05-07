package com.jianhui.project.harbor.server.task;

import com.jianhui.project.harbor.common.mq.RedisMQConsumer;
import com.jianhui.project.harbor.server.netty.IMServerGroup;
import org.springframework.beans.factory.annotation.Value;

/**
 * 抽象类
 * 实现类分别实现拉取群聊，私聊，系统消息
 */
public abstract class AbstractPullMessageTask extends RedisMQConsumer {

    @Value("${spring.application.name}")
    String appName;

    @Override
    public String generateKey() {
        return String.join(":",super.generateKey(), IMServerGroup.serverId + "");
    }


}
