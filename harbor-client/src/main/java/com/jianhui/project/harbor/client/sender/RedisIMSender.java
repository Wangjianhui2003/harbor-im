package com.jianhui.project.harbor.client.sender;

import com.jianhui.project.harbor.common.model.IMPrivateMessage;
import com.jianhui.project.harbor.common.mq.RedisMQTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisIMSender {

    private final RedisMQTemplate redisMQTemplate;

    @Value("${spring.application.name}")
    private String appName;

    public<T> void sendPrivateMessage(IMPrivateMessage<T> message){

    }
}
