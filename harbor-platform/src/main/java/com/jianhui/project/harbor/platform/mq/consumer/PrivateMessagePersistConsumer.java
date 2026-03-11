package com.jianhui.project.harbor.platform.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.model.PrivateMessageCreatedEvent;
import com.jianhui.project.harbor.platform.dao.entity.PrivateMessage;
import com.jianhui.project.harbor.platform.dao.mapper.PrivateMessageMapper;
import com.jianhui.project.harbor.platform.config.props.PrivateMessageMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMessagePersistConsumer implements ApplicationRunner {

    @Value("${rocketmq.name-server}")
    private String nameServerAddr;

    private final PrivateMessageMapper privateMessageMapper;
    private final PrivateMessageMQProperties mqProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DefaultMQPushConsumer consumer =
                new DefaultMQPushConsumer(IMMQConstant.PRIVATE_PERSIST_CONSUMER_GROUP);

        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(IMMQConstant.PRIVATE_PERSIST_TOPIC, "*");
        configureConsumer(consumer, mqProperties.getPersist());

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                List<PrivateMessageCreatedEvent> events = new ArrayList<>(list.size());
                for (MessageExt msg : list) {
                    PrivateMessageCreatedEvent event = parseEvent(msg);
                    if (event != null) {
                        events.add(event);
                    }
                }
                if (events.isEmpty()) {
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                try {
                    persistBatch(events);
                } catch (Exception e) {
                    log.error("私聊消息批量持久化失败，稍后重试，size:{}", events.size(), e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
        log.info("私聊消息持久化消费者启动成功");
    }

    private void persistBatch(List<PrivateMessageCreatedEvent> events) {
        List<PrivateMessage> messages = events.stream().map(event -> {
            PrivateMessage message = new PrivateMessage();
            message.setId(event.getId());
            message.setSendId(event.getSendId());
            message.setRecvId(event.getRecvId());
            message.setContent(event.getContent());
            message.setType(event.getType());
            message.setStatus(event.getStatus());
            message.setSendTime(event.getSendTime());
            return message;
        }).toList();
        privateMessageMapper.batchInsertIgnore(messages);
    }

    private PrivateMessageCreatedEvent parseEvent(MessageExt msg) {
        try {
            byte[] body = msg.getBody();
            String string = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(body)).toString();
            if (StringUtils.isBlank(string)) {
                log.warn("收到空的私聊消息创建事件，msgId:{}", msg.getMsgId());
                return null;
            }
            PrivateMessageCreatedEvent event = JSON.parseObject(string, PrivateMessageCreatedEvent.class);
            if (event == null || event.getId() == null) {
                log.warn("私聊消息创建事件解析失败，mqMsgId:{}, body:{}", msg.getMsgId(), string);
                return null;
            }
            return event;
        } catch (Exception e) {
            log.error("私聊消息创建事件解析异常，mqMsgId:{}", msg.getMsgId(), e);
            return null;
        }
    }

    private void configureConsumer(DefaultMQPushConsumer consumer, PrivateMessageMQProperties.Consumer properties) {
        consumer.setConsumeThreadMin(properties.getConsumeThreadMin());
        consumer.setConsumeThreadMax(properties.getConsumeThreadMax());
        consumer.setConsumeMessageBatchMaxSize(properties.getConsumeMessageBatchMaxSize());
        consumer.setPullBatchSize(properties.getPullBatchSize());
    }
}
