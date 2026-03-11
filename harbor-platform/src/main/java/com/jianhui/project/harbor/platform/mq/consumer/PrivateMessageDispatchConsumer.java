package com.jianhui.project.harbor.platform.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.constant.IMRedisKey;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.enums.IMTerminalType;
import com.jianhui.project.harbor.common.model.IMBatchRecvInfo;
import com.jianhui.project.harbor.common.model.IMRecvInfo;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.common.mq.RedisMQTemplate;
import com.jianhui.project.harbor.common.model.PrivateMessageCreatedEvent;
import com.jianhui.project.harbor.platform.config.props.PrivateMessageMQProperties;
import com.jianhui.project.harbor.platform.dto.response.PrivateMessageRespDTO;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMessageDispatchConsumer implements ApplicationRunner {

    @Value("${rocketmq.name-server}")
    private String nameServerAddr;

    @Value("${spring.application.name}")
    private String appName;

    private final RedisMQTemplate redisMQTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final PrivateMessageMQProperties mqProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DefaultMQPushConsumer consumer =
                new DefaultMQPushConsumer(IMMQConstant.PRIVATE_DISPATCH_CONSUMER_GROUP);

        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(IMMQConstant.PRIVATE_PERSIST_TOPIC, "*");
        configureConsumer(consumer, mqProperties.getDispatch());

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
                try {
                    dispatchBatch(events);
                } catch (Exception e) {
                    log.error("私聊消息批量投递失败，稍后重试，size:{}", events.size(), e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
        log.info("私聊消息投递消费者启动成功");
    }

    private void dispatchBatch(List<PrivateMessageCreatedEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        Map<String, IMUserInfo> routeKeys = collectRouteKeys(events);
        List<Object> serverIds = redisMQTemplate.opsForValue().multiGet(routeKeys.keySet());
        Map<String, Integer> routeTable = buildRouteTable(routeKeys, serverIds);
        Map<Integer, List<IMRecvInfo>> serverMessages = buildServerMessages(events, routeTable);
        for (Map.Entry<Integer, List<IMRecvInfo>> entry : serverMessages.entrySet()) {
            IMBatchRecvInfo batchRecvInfo = new IMBatchRecvInfo();
            batchRecvInfo.setMessages(entry.getValue());
            rocketMQTemplate.syncSend(IMMQConstant.PRIVATE_MSG_TOPIC_PREFIX + entry.getKey(),
                    JSON.toJSONString(batchRecvInfo),
                    5000);
        }
    }

    private Map<String, IMUserInfo> collectRouteKeys(List<PrivateMessageCreatedEvent> events) {
        Map<String, IMUserInfo> routeKeys = new LinkedHashMap<>();
        for (PrivateMessageCreatedEvent event : events) {
            for (Integer terminal : IMTerminalType.codes()) {
                String key = buildRouteKey(event.getRecvId(), terminal);
                routeKeys.putIfAbsent(key, new IMUserInfo(event.getRecvId(), terminal));
            }
            if (Boolean.TRUE.equals(event.getSendToSelf())) {
                for (Integer terminal : IMTerminalType.codes()) {
                    if (terminal.equals(event.getSenderTerminal())) {
                        continue;
                    }
                    String key = buildRouteKey(event.getSendId(), terminal);
                    routeKeys.putIfAbsent(key, new IMUserInfo(event.getSendId(), terminal));
                }
            }
        }
        return routeKeys;
    }

    private Map<String, Integer> buildRouteTable(Map<String, IMUserInfo> routeKeys, List<Object> serverIds) {
        Map<String, Integer> routeTable = new HashMap<>(routeKeys.size());
        int idx = 0;
        for (String key : routeKeys.keySet()) {
            Number serverIdNum = serverIds == null ? null : (Number) serverIds.get(idx++);
            if (serverIdNum != null) {
                routeTable.put(key, serverIdNum.intValue());
            }
        }
        return routeTable;
    }

    private Map<Integer, List<IMRecvInfo>> buildServerMessages(List<PrivateMessageCreatedEvent> events, Map<String, Integer> routeTable) {
        Map<Integer, List<IMRecvInfo>> serverMessages = new HashMap<>();
        for (PrivateMessageCreatedEvent event : events) {
            PrivateMessageRespDTO messageInfo = BeanUtils.copyProperties(event, PrivateMessageRespDTO.class);
            IMUserInfo sender = new IMUserInfo(event.getSendId(), event.getSenderTerminal());

            Map<Integer, List<IMUserInfo>> receiverMap = new HashMap<>();
            for (Integer terminal : IMTerminalType.codes()) {
                Integer serverId = routeTable.get(buildRouteKey(event.getRecvId(), terminal));
                if (serverId != null) {
                    receiverMap.computeIfAbsent(serverId, key -> new ArrayList<>())
                            .add(new IMUserInfo(event.getRecvId(), terminal));
                }
            }
            addServerMessages(serverMessages, receiverMap, sender, messageInfo, Boolean.TRUE.equals(event.getSendBack()));

            if (Boolean.TRUE.equals(event.getSendToSelf())) {
                Map<Integer, List<IMUserInfo>> selfMap = new HashMap<>();
                for (Integer terminal : IMTerminalType.codes()) {
                    if (terminal.equals(event.getSenderTerminal())) {
                        continue;
                    }
                    Integer serverId = routeTable.get(buildRouteKey(event.getSendId(), terminal));
                    if (serverId != null) {
                        selfMap.computeIfAbsent(serverId, key -> new ArrayList<>())
                                .add(new IMUserInfo(event.getSendId(), terminal));
                    }
                }
                addServerMessages(serverMessages, selfMap, sender, messageInfo, false);
            }
        }
        return serverMessages;
    }

    private void addServerMessages(Map<Integer, List<IMRecvInfo>> serverMessages,
                                   Map<Integer, List<IMUserInfo>> receiverMap,
                                   IMUserInfo sender,
                                   PrivateMessageRespDTO messageInfo,
                                   boolean sendBack) {
        for (Map.Entry<Integer, List<IMUserInfo>> entry : receiverMap.entrySet()) {
            IMRecvInfo recvInfo = new IMRecvInfo();
            recvInfo.setCmd(IMCmdType.PRIVATE_MESSAGE.code());
            recvInfo.setSender(sender);
            recvInfo.setReceivers(entry.getValue());
            recvInfo.setIsSendBack(sendBack);
            recvInfo.setServiceName(appName);
            recvInfo.setData(messageInfo);
            serverMessages.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(recvInfo);
        }
    }

    private String buildRouteKey(Long userId, Integer terminal) {
        return String.join(":", IMRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
    }

    private PrivateMessageCreatedEvent parseEvent(MessageExt msg) {
        try {
            byte[] body = msg.getBody();
            String string = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(body)).toString();
            if (StringUtils.isBlank(string)) {
                log.warn("收到空的私聊消息投递事件，msgId:{}", msg.getMsgId());
                return null;
            }
            PrivateMessageCreatedEvent event = JSON.parseObject(string, PrivateMessageCreatedEvent.class);
            if (event == null || event.getId() == null) {
                log.warn("私聊消息投递事件解析失败，mqMsgId:{}, body:{}", msg.getMsgId(), string);
                return null;
            }
            return event;
        } catch (Exception e) {
            log.error("私聊消息投递事件解析异常，mqMsgId:{}", msg.getMsgId(), e);
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
