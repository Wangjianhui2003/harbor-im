package com.jianhui.project.harbor.platform.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.model.GroupMessageCreatedEvent;
import com.jianhui.project.harbor.common.util.CommaTextUtils;
import com.jianhui.project.harbor.platform.config.props.GroupMessageMQProperties;
import com.jianhui.project.harbor.platform.dao.entity.GroupMessage;
import com.jianhui.project.harbor.platform.dao.mapper.GroupMessageMapper;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupMessagePersistConsumer implements ApplicationRunner {

    @Value("${rocketmq.name-server}")
    private String nameServerAddr;

    private final GroupMessageMapper groupMessageMapper;
    private final GroupMessageMQProperties mqProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DefaultMQPushConsumer consumer =
                new DefaultMQPushConsumer(IMMQConstant.GROUP_PERSIST_CONSUMER_GROUP);

        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(IMMQConstant.GROUP_PERSIST_TOPIC, "*");
        configureConsumer(consumer, mqProperties.getPersist());

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                List<GroupMessageCreatedEvent> events = new ArrayList<>(list.size());
                for (MessageExt msg : list) {
                    GroupMessageCreatedEvent event = parseEvent(msg);
                    if (event != null) {
                        events.add(event);
                    }
                }
                if (events.isEmpty()) {
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                try {
                    persistBatch(events);
                } catch (DuplicateKeyException e) {
                    log.warn("群聊消息批量持久化存在重复主键，size:{}", events.size(), e);
                } catch (Exception e) {
                    log.error("群聊消息批量持久化失败，稍后重试，size:{}", events.size(), e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
        log.info("群聊消息持久化消费者启动成功");
    }

    private void persistBatch(List<GroupMessageCreatedEvent> events) {
        List<GroupMessage> messages = events.stream().map(event -> {
            GroupMessage message = new GroupMessage();
            message.setId(event.getId());
            message.setGroupId(event.getGroupId());
            message.setSendId(event.getSendId());
            message.setSendNickname(event.getSendNickname());
            message.setRecvIds("");
            message.setContent(event.getContent());
            message.setAtUserIds(CommaTextUtils.asText(event.getAtUserIds()));
            message.setReceipt(event.getReceipt());
            message.setReceiptOk(event.getReceiptOk());
            message.setType(event.getType());
            message.setStatus(event.getStatus());
            message.setSendTime(event.getSendTime());
            return message;
        }).toList();
        groupMessageMapper.batchInsertIgnore(messages);
    }

    private GroupMessageCreatedEvent parseEvent(MessageExt msg) {
        try {
            byte[] body = msg.getBody();
            String string = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(body)).toString();
            if (StringUtils.isBlank(string)) {
                log.warn("收到空的群聊消息创建事件，mqMsgId:{}", msg.getMsgId());
                return null;
            }
            GroupMessageCreatedEvent event = JSON.parseObject(string, GroupMessageCreatedEvent.class);
            if (event == null || event.getId() == null) {
                log.warn("群聊消息创建事件解析失败，mqMsgId:{}, body:{}", msg.getMsgId(), string);
                return null;
            }
            return event;
        } catch (Exception e) {
            log.error("群聊消息创建事件解析异常，mqMsgId:{}", msg.getMsgId(), e);
            return null;
        }
    }

    private void configureConsumer(DefaultMQPushConsumer consumer, GroupMessageMQProperties.Consumer properties) {
        consumer.setConsumeThreadMin(properties.getConsumeThreadMin());
        consumer.setConsumeThreadMax(properties.getConsumeThreadMax());
        consumer.setConsumeMessageBatchMaxSize(properties.getConsumeMessageBatchMaxSize());
        consumer.setPullBatchSize(properties.getPullBatchSize());
    }
}
