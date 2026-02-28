package com.jianhui.project.harbor.platform.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jianhui.project.harbor.client.IMClient;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.model.IMPrivateMessage;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.common.model.PrivateMessageCreatedEvent;
import com.jianhui.project.harbor.platform.dao.entity.PrivateMessage;
import com.jianhui.project.harbor.platform.dao.mapper.PrivateMessageMapper;
import com.jianhui.project.harbor.platform.dto.response.PrivateMessageRespDTO;
import com.jianhui.project.harbor.platform.util.BeanUtils;
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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMessagePersistConsumer implements ApplicationRunner {

    @Value("${rocketmq.name-server}")
    private String nameServerAddr;

    private final PrivateMessageMapper privateMessageMapper;
    private final IMClient imClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DefaultMQPushConsumer consumer =
                new DefaultMQPushConsumer(IMMQConstant.PRIVATE_PERSIST_CONSUMER_GROUP);

        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(IMMQConstant.PRIVATE_PERSIST_TOPIC, "*");

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                for (MessageExt msg : list) {
                    try {
                        byte[] body = msg.getBody();
                        String string = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(body)).toString();
                        if (StringUtils.isBlank(string)) {
                            log.warn("收到空的私聊消息创建事件，msgId:{}", msg.getMsgId());
                            continue;
                        }
                        PrivateMessageCreatedEvent event = JSON.parseObject(string, PrivateMessageCreatedEvent.class);
                        if (event == null || event.getId() == null) {
                            log.warn("私聊消息创建事件解析失败，mqMsgId:{}, body:{}", msg.getMsgId(), string);
                            continue;
                        }
                        try {
                            persistAndDispatch(event);
                        } catch (DuplicateKeyException e) {
                            log.warn("私聊消息重复持久化，消息id:{},发送者:{},接收者:{}",
                                    event.getId(),
                                    event.getSendId(),
                                    event.getRecvId());
                        }
                    } catch (Exception e) {
                        log.error("私聊消息异步持久化失败，稍后重试", e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
        log.info("私聊消息持久化消费者启动成功");
    }

    private void persistAndDispatch(PrivateMessageCreatedEvent event) {
        PrivateMessage message = BeanUtils.copyProperties(event, PrivateMessage.class);
        privateMessageMapper.insert(message);

        PrivateMessageRespDTO messageInfo = BeanUtils.copyProperties(message, PrivateMessageRespDTO.class);
        IMPrivateMessage<PrivateMessageRespDTO> imMessage = new IMPrivateMessage<>();
        imMessage.setSender(new IMUserInfo(event.getSendId(), event.getSenderTerminal()));
        imMessage.setRecvId(event.getRecvId());
        imMessage.setSendToSelf(Boolean.TRUE.equals(event.getSendToSelf()));
        imMessage.setIsSendBack(Boolean.TRUE.equals(event.getSendBack()));
        imMessage.setData(messageInfo);
        imClient.sendPrivateMessage(imMessage);

        long lagMs = event.getSendTime() == null ? -1L : System.currentTimeMillis() - event.getSendTime().getTime();
        log.info("私聊消息持久化并投递成功，消息id:{},发送者:{},接收者:{},lagMs:{}",
                event.getId(),
                event.getSendId(),
                event.getRecvId(),
                lagMs);
    }
}
