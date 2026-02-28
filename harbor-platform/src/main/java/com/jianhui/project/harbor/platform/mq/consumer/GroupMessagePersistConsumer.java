package com.jianhui.project.harbor.platform.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jianhui.project.harbor.client.IMClient;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.model.GroupMessageCreatedEvent;
import com.jianhui.project.harbor.common.model.IMGroupMessage;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.common.util.CommaTextUtils;
import com.jianhui.project.harbor.platform.dao.entity.GroupMessage;
import com.jianhui.project.harbor.platform.dao.mapper.GroupMessageMapper;
import com.jianhui.project.harbor.platform.dto.response.GroupMessageRespDTO;
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
public class GroupMessagePersistConsumer implements ApplicationRunner {

    @Value("${rocketmq.name-server}")
    private String nameServerAddr;

    private final GroupMessageMapper groupMessageMapper;
    private final IMClient imClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DefaultMQPushConsumer consumer =
                new DefaultMQPushConsumer(IMMQConstant.GROUP_PERSIST_CONSUMER_GROUP);

        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(IMMQConstant.GROUP_PERSIST_TOPIC, "*");

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                for (MessageExt msg : list) {
                    try {
                        byte[] body = msg.getBody();
                        String string = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(body)).toString();
                        if (StringUtils.isBlank(string)) {
                            log.warn("收到空的群聊消息创建事件，mqMsgId:{}", msg.getMsgId());
                            continue;
                        }
                        GroupMessageCreatedEvent event = JSON.parseObject(string, GroupMessageCreatedEvent.class);
                        if (event == null || event.getId() == null) {
                            log.warn("群聊消息创建事件解析失败，mqMsgId:{}, body:{}", msg.getMsgId(), string);
                            continue;
                        }
                        try {
                            persistAndDispatch(event);
                        } catch (DuplicateKeyException e) {
                            log.warn("群聊消息重复持久化，消息id:{},发送者:{},群聊id:{}",
                                    event.getId(),
                                    event.getSendId(),
                                    event.getGroupId());
                        }
                    } catch (Exception e) {
                        log.error("群聊消息异步持久化失败，稍后重试", e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
        log.info("群聊消息持久化消费者启动成功");
    }

    private void persistAndDispatch(GroupMessageCreatedEvent event) {
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
        groupMessageMapper.insert(message);

        GroupMessageRespDTO messageInfo = BeanUtils.copyProperties(message, GroupMessageRespDTO.class);
        messageInfo.setAtUserIds(event.getAtUserIds());
        IMGroupMessage<GroupMessageRespDTO> imMessage = new IMGroupMessage<>();
        imMessage.setSender(new IMUserInfo(event.getSendId(), event.getSenderTerminal()));
        imMessage.setRecvIds(event.getDeliveryRecvIds());
        imMessage.setSendToSelf(Boolean.TRUE.equals(event.getSendToSelf()));
        imMessage.setIsSendBack(Boolean.TRUE.equals(event.getSendBack()));
        imMessage.setData(messageInfo);
        imClient.sendGroupMessage(imMessage);

        long lagMs = event.getSendTime() == null ? -1L : System.currentTimeMillis() - event.getSendTime().getTime();
        log.info("群聊消息持久化并投递成功，消息id:{},发送者:{},群聊id:{},接收人数:{},lagMs:{}",
                event.getId(),
                event.getSendId(),
                event.getGroupId(),
                event.getDeliveryRecvIds() == null ? 0 : event.getDeliveryRecvIds().size(),
                lagMs);
    }
}
