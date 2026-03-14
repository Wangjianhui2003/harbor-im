package com.jianhui.project.harbor.server.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMBatchRecvInfo;
import com.jianhui.project.harbor.common.model.IMRecvInfo;
import com.jianhui.project.harbor.server.config.props.GroupMessageMQProperties;
import com.jianhui.project.harbor.server.event.IMServerReadyEvent;
import com.jianhui.project.harbor.server.netty.IMServerGroup;
import com.jianhui.project.harbor.server.netty.processor.ProcessorFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * RedisServer启动后再启动消息队列
 */

@Slf4j
@Component
public class GroupMessageConsumer implements ApplicationListener<IMServerReadyEvent> {

    @Value("${rocketmq.name-server}")
    private String nameServerAddr;

    private final GroupMessageMQProperties mqProperties;

    public GroupMessageConsumer(GroupMessageMQProperties mqProperties) {
        this.mqProperties = mqProperties;
    }

    @Override
    public void onApplicationEvent(IMServerReadyEvent event) {
        try {
            DefaultMQPushConsumer consumer =
                    new DefaultMQPushConsumer(IMMQConstant.GROUP_MSG_CONSUMER_GROUP + IMServerGroup.serverId);
            consumer.setNamesrvAddr(nameServerAddr);
            consumer.subscribe(IMMQConstant.GROUP_MSG_TOPIC_PREFIX + IMServerGroup.serverId, "*");
            configureConsumer(consumer, mqProperties.getConsumer());

            consumer.registerMessageListener((MessageListenerConcurrently) (list, consumeConcurrentlyContext) -> {
                for (MessageExt msg : list) {
                    byte[] body = msg.getBody();
                    String string = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(body)).toString();
                    JSONObject jsonObject = JSON.parseObject(string);
                    if (jsonObject.containsKey("messages")) {
                        IMBatchRecvInfo batchRecvInfo = jsonObject.toJavaObject(IMBatchRecvInfo.class);
                        for (IMRecvInfo recvInfo : batchRecvInfo.getMessages()) {
                            ProcessorFactory.getProcessor(IMCmdType.GROUP_MESSAGE).process(recvInfo);
                        }
                    } else {
                        IMRecvInfo recvInfo = jsonObject.toJavaObject(IMRecvInfo.class);
                        ProcessorFactory.getProcessor(IMCmdType.GROUP_MESSAGE).process(recvInfo);
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();
            log.info("Server:{} 群聊信息消费者启动成功", IMServerGroup.serverId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void configureConsumer(DefaultMQPushConsumer consumer, GroupMessageMQProperties.Consumer properties) {
        consumer.setConsumeThreadMin(properties.getConsumeThreadMin());
        consumer.setConsumeThreadMax(properties.getConsumeThreadMax());
        consumer.setConsumeMessageBatchMaxSize(properties.getConsumeMessageBatchMaxSize());
        consumer.setPullBatchSize(properties.getPullBatchSize());
    }
}
