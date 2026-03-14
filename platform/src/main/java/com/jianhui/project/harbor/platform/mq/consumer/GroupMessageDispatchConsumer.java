package com.jianhui.project.harbor.platform.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.constant.IMRedisKey;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.enums.IMTerminalType;
import com.jianhui.project.harbor.common.model.GroupMessageCreatedEvent;
import com.jianhui.project.harbor.common.model.IMBatchRecvInfo;
import com.jianhui.project.harbor.common.model.IMRecvInfo;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.common.mq.RedisMQTemplate;
import com.jianhui.project.harbor.common.util.ThreadPoolExecutorFactory;
import com.jianhui.project.harbor.platform.config.props.GroupMessageMQProperties;
import com.jianhui.project.harbor.platform.dto.response.GroupMessageRespDTO;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupMessageDispatchConsumer implements ApplicationRunner {

    @Value("${rocketmq.name-server}")
    private String nameServerAddr;

    @Value("${spring.application.name}")
    private String appName;

    private final RedisMQTemplate redisMQTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final GroupMessageMQProperties mqProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DefaultMQPushConsumer consumer =
                new DefaultMQPushConsumer(IMMQConstant.GROUP_DISPATCH_CONSUMER_GROUP);

        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(IMMQConstant.GROUP_PERSIST_TOPIC, "*");
        configureConsumer(consumer, mqProperties.getDispatch());

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
                try {
                    dispatchBatch(events);
                } catch (Exception e) {
                    log.error("群聊消息批量投递失败，稍后重试，size:{}", events.size(), e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
        log.info("群聊消息投递消费者启动成功");
    }

    private void dispatchBatch(List<GroupMessageCreatedEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        Map<String, IMUserInfo> routeKeys = collectRouteKeys(events);
        List<Object> serverIds = redisMQTemplate.opsForValue().multiGet(routeKeys.keySet());
        Map<String, Integer> routeTable = buildRouteTable(routeKeys, serverIds);
        Map<Integer, List<IMRecvInfo>> serverMessages = buildServerMessages(events, routeTable);
        serverMessages.forEach(this::sendBatchAsync);
    }

    private void sendBatchAsync(Integer serverId, List<IMRecvInfo> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        IMBatchRecvInfo batchRecvInfo = new IMBatchRecvInfo();
        batchRecvInfo.setMessages(messages);
        String payload = JSON.toJSONString(batchRecvInfo);
        sendBatchAsync(serverId, payload, messages.size(), 1, false);
    }

    private void sendBatchAsync(Integer serverId, String payload, int messageCount, int attempt, boolean retryContext) {
        String topic = IMMQConstant.GROUP_MSG_TOPIC_PREFIX + serverId;
        try {
            rocketMQTemplate.asyncSend(topic, payload, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.debug("群聊消息批量异步投递成功，serverId:{}, size:{}, attempt:{}, msgId:{}",
                            serverId, messageCount, attempt, sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable throwable) {
                    handleAsyncSendFailure(serverId, payload, messageCount, attempt, throwable);
                }
            }, mqProperties.getRetry().getSendTimeoutMs());
        } catch (Exception e) {
            if (!retryContext) {
                throw e;
            }
            handleAsyncSendFailure(serverId, payload, messageCount, attempt, e);
        }
    }

    private void handleAsyncSendFailure(Integer serverId, String payload, int messageCount, int attempt, Throwable throwable) {
        int maxAttempts = mqProperties.getRetry().getMaxAttempts();
        if (attempt >= maxAttempts) {
            log.error("群聊消息批量异步投递失败，补偿重发次数已耗尽，serverId:{}, size:{}, attempt:{}/{}",
                    serverId, messageCount, attempt, maxAttempts, throwable);
            return;
        }
        long delayMs = mqProperties.getRetry().getRetryDelayMs() * attempt;
        log.warn("群聊消息批量异步投递失败，准备补偿重发，serverId:{}, size:{}, nextAttempt:{}/{}, delayMs:{}",
                serverId, messageCount, attempt + 1, maxAttempts, delayMs, throwable);
        ThreadPoolExecutorFactory.getThreadPoolExecutor().schedule(
                () -> sendBatchAsync(serverId, payload, messageCount, attempt + 1, true),
                delayMs,
                TimeUnit.MILLISECONDS
        );
    }

    private Map<String, IMUserInfo> collectRouteKeys(List<GroupMessageCreatedEvent> events) {
        Map<String, IMUserInfo> routeKeys = new LinkedHashMap<>();
        for (GroupMessageCreatedEvent event : events) {
            if (event.getDeliveryRecvIds() != null) {
                for (Long recvId : event.getDeliveryRecvIds()) {
                    for (Integer terminal : IMTerminalType.codes()) {
                        String key = buildRouteKey(recvId, terminal);
                        routeKeys.putIfAbsent(key, new IMUserInfo(recvId, terminal));
                    }
                }
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

    private Map<Integer, List<IMRecvInfo>> buildServerMessages(List<GroupMessageCreatedEvent> events, Map<String, Integer> routeTable) {
        Map<Integer, List<IMRecvInfo>> serverMessages = new HashMap<>();
        for (GroupMessageCreatedEvent event : events) {
            GroupMessageRespDTO messageInfo = BeanUtils.copyProperties(event, GroupMessageRespDTO.class);
            IMUserInfo sender = new IMUserInfo(event.getSendId(), event.getSenderTerminal());

            Map<Integer, List<IMUserInfo>> receiverMap = new HashMap<>();
            if (event.getDeliveryRecvIds() != null) {
                for (Long recvId : event.getDeliveryRecvIds()) {
                    for (Integer terminal : IMTerminalType.codes()) {
                        Integer serverId = routeTable.get(buildRouteKey(recvId, terminal));
                        if (serverId != null) {
                            receiverMap.computeIfAbsent(serverId, key -> new ArrayList<>())
                                    .add(new IMUserInfo(recvId, terminal));
                        }
                    }
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
                                   GroupMessageRespDTO messageInfo,
                                   boolean sendBack) {
        for (Map.Entry<Integer, List<IMUserInfo>> entry : receiverMap.entrySet()) {
            IMRecvInfo recvInfo = new IMRecvInfo();
            recvInfo.setCmd(IMCmdType.GROUP_MESSAGE.code());
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

    private GroupMessageCreatedEvent parseEvent(MessageExt msg) {
        try {
            byte[] body = msg.getBody();
            String string = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(body)).toString();
            if (StringUtils.isBlank(string)) {
                log.warn("收到空的群聊消息投递事件，msgId:{}", msg.getMsgId());
                return null;
            }
            GroupMessageCreatedEvent event = JSON.parseObject(string, GroupMessageCreatedEvent.class);
            if (event == null || event.getId() == null) {
                log.warn("群聊消息投递事件解析失败，mqMsgId:{}, body:{}", msg.getMsgId(), string);
                return null;
            }
            return event;
        } catch (Exception e) {
            log.error("群聊消息投递事件解析异常，mqMsgId:{}", msg.getMsgId(), e);
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
