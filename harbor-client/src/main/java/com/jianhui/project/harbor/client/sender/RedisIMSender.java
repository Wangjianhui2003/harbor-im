package com.jianhui.project.harbor.client.sender;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.constant.IMRedisKey;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.enums.IMSendCode;
import com.jianhui.project.harbor.common.enums.IMTerminalType;
import com.jianhui.project.harbor.common.model.*;
import com.jianhui.project.harbor.common.mq.RedisMQTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisIMSender implements IMSender {

    private final RedisMQTemplate redisMQTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public <T> void sendSystemMessage(IMSystemMessage<T> message) {
        //组合所有接收id和terminal
        Map<String, IMUserInfo> sendMap = new HashMap<>();
        for (Integer terminal : message.getRecvTerminals()) {
            message.getRecvIds().forEach(id -> {
                String key = String.join(":", IMRedisKey.IM_USER_SERVER_ID, id.toString(), terminal.toString());
                sendMap.put(key, new IMUserInfo(id, terminal));
            });
        }
        //拉取用户id+terminal所在server的id
        List<Object> serverIds = redisMQTemplate.opsForValue().multiGet(sendMap.keySet());
        // serverId和对应的所有UserInfo 格式:map<服务器id,list<接收方>>
        Map<Integer, List<IMUserInfo>> serverMap = new HashMap<>();
        //离线用户
        List<IMUserInfo> offLineUsers = new LinkedList<>();
        int idx = 0;
        for (Map.Entry<String, IMUserInfo> entry : sendMap.entrySet()) {
            //当前用户的server id
            Number serverIdNum = (Number) serverIds.get(idx);
            Integer serverId = serverIdNum != null ? serverIdNum.intValue() : null;
            if (serverId != null) {
                List<IMUserInfo> list = serverMap.computeIfAbsent(serverId, o -> new LinkedList<>());
                list.add(entry.getValue());
            } else {
                offLineUsers.add(entry.getValue());
            }
        }
        //
        for (Map.Entry<Integer, List<IMUserInfo>> entry : serverMap.entrySet()) {
            IMRecvInfo recvInfo = new IMRecvInfo();
            recvInfo.setCmd(IMCmdType.SYSTEM_MESSAGE.code());
            recvInfo.setReceivers(entry.getValue());
            recvInfo.setServiceName(appName);
            recvInfo.setIsSendBack(message.getIsSendBack());
            recvInfo.setData(message.getData());
            //推送到队列
            asyncSendToMQ(IMMQConstant.SYSTEM_MSG_TOPIC_PREFIX + entry.getKey(), recvInfo);
        }
        // 对离线用户回复消息状态
        if (message.getIsSendBack() && offLineUsers.isEmpty()) {
            //TODO:
        }
    }

    @Override
    public <T> void sendPrivateMessage(IMPrivateMessage<T> message) {
        List<IMSendResult> results = new LinkedList<>();
        if (message.getRecvId() != null) {
            for (Integer terminal : message.getRecvTerminals()) {
                // 获取server id
                String key = String.join(":", IMRedisKey.IM_USER_SERVER_ID, message.getRecvId().toString(), terminal.toString());
                Number serverIdNum = (Number) redisMQTemplate.opsForValue().get(key);
                Integer serverId = serverIdNum != null ? serverIdNum.intValue() : null;
                if (serverId != null) {
                    IMRecvInfo recvInfo = new IMRecvInfo();
                    recvInfo.setCmd(IMCmdType.PRIVATE_MESSAGE.code());
                    recvInfo.setServiceName(appName);
                    recvInfo.setSender(message.getSender());
                    recvInfo.setIsSendBack(message.getIsSendBack());
                    recvInfo.setReceivers(List.of(new IMUserInfo(message.getRecvId(), terminal)));
                    recvInfo.setData(message.getData());
                    asyncSendToMQ(IMMQConstant.PRIVATE_MSG_TOPIC_PREFIX + serverId, recvInfo);
                } else {
                    //离线用户
                    IMSendResult result = new IMSendResult();
                    result.setSender(message.getSender());
                    result.setReceiver(new IMUserInfo(message.getRecvId(), terminal));
                    result.setCode(IMSendCode.NOT_ONLINE.code());
                    result.setData(message.getData());
                    results.add(result);
                }
            }
        }
        //推送给自己其他客户端
        if (message.getSendToSelf()) {
            for (Integer terminal : IMTerminalType.codes()) {
                IMUserInfo sender = message.getSender();
                //相同terminal不发
                if (sender.getTerminal().equals(terminal)) {
                    continue;
                }
                String key = String.join(":", IMRedisKey.IM_USER_SERVER_ID, sender.getId().toString(), sender.getTerminal().toString());
                Number serverIdNum = (Number) redisMQTemplate.opsForValue().get(key);
                Integer serverId = serverIdNum != null ? serverIdNum.intValue() : null;
                if (serverId != null) {
                    IMRecvInfo recvInfo = new IMRecvInfo();
                    // 自己的消息不需要回推消息发送结果
                    recvInfo.setIsSendBack(false);
                    recvInfo.setCmd(IMCmdType.PRIVATE_MESSAGE.code());
                    recvInfo.setSender(message.getSender());
                    recvInfo.setReceivers(Collections.singletonList(new IMUserInfo(message.getSender().getId(), terminal)));
                    recvInfo.setData(message.getData());
                    //发送
                    asyncSendToMQ(IMMQConstant.PRIVATE_MSG_TOPIC_PREFIX + serverId, recvInfo);
                }
            }
        }
        if (message.getIsSendBack() && !results.isEmpty()) {
            //TODO:multicast
        }
    }

    @Override
    public <T> void sendGroupMessage(IMGroupMessage<T> message) {
        log.info("发送群消息:{}", message);
        Map<String, IMUserInfo> sendMap = new HashMap<>();
        for (Integer terminal : message.getRecvTerminals()) {
            for (Long recvId : message.getRecvIds()) {
                String key = String.join(":", IMRedisKey.IM_USER_SERVER_ID, recvId.toString(), terminal.toString());
                sendMap.put(key, new IMUserInfo(recvId, terminal));
            }
        }
        //批量获取serverId
        List<Object> serverIds = redisMQTemplate.opsForValue().multiGet(sendMap.keySet());
        Map<Integer, List<IMUserInfo>> serverMap = new HashMap<>();
        List<IMUserInfo> offLineUsers = new LinkedList<>();
        //按serverId分组
        int idx = 0;
        for (Map.Entry<String, IMUserInfo> entry : sendMap.entrySet()) {
            Number serverIdNum = (Number) serverIds.get(idx++);
            Integer serverId = serverIdNum != null ? serverIdNum.intValue() : null;
            if (serverId != null) {
                List<IMUserInfo> list = serverMap.computeIfAbsent(serverId, o -> new LinkedList<>());
                list.add(entry.getValue());
            } else {
                // 加入离线列表
                offLineUsers.add(entry.getValue());
            }
        }
        //按serverId分别投送给该server上的用户
        for (Map.Entry<Integer, List<IMUserInfo>> entry : serverMap.entrySet()) {
            IMRecvInfo recvInfo = new IMRecvInfo();
            recvInfo.setCmd(IMCmdType.GROUP_MESSAGE.code());
            recvInfo.setSender(message.getSender());
            recvInfo.setReceivers(new LinkedList<>(entry.getValue()));
            recvInfo.setIsSendBack(message.getIsSendBack());
            recvInfo.setServiceName(appName);
            recvInfo.setData(message.getData());
            // 推送至队列
            asyncSendToMQ(IMMQConstant.GROUP_MSG_TOPIC_PREFIX + entry.getKey(), recvInfo);
        }
        // 推送给自己的其他终端
        if (message.getSendToSelf()) {
            for (Integer terminal : IMTerminalType.codes()) {
                if (terminal.equals(message.getSender().getTerminal())) {
                    continue;
                }
                // 获取终端连接的channelId
                String key = String.join(":", IMRedisKey.IM_USER_SERVER_ID, message.getSender().getId().toString(), terminal.toString());
                Number serverIdNum = (Number) redisMQTemplate.opsForValue().get(key);
                Integer serverId = serverIdNum != null ? serverIdNum.intValue() : null;
                // 如果终端在线，将数据存储至redis，等待拉取推送
                if (serverId != null) {
                    IMRecvInfo recvInfo = new IMRecvInfo();
                    recvInfo.setCmd(IMCmdType.GROUP_MESSAGE.code());
                    recvInfo.setSender(message.getSender());
                    recvInfo.setReceivers(Collections.singletonList(new IMUserInfo(message.getSender().getId(), terminal)));
                    // 自己的消息不需要回推消息结果
                    recvInfo.setIsSendBack(false);
                    recvInfo.setData(message.getData());
                    asyncSendToMQ(IMMQConstant.GROUP_MSG_TOPIC_PREFIX + serverId, recvInfo);
                }
            }
        }
        // 对离线用户回复消息状态
        if (message.getIsSendBack() && !offLineUsers.isEmpty()) {
            //TODO:group offline
        }
    }

    @Override
    public Map<Long, List<IMTerminalType>> getOnlineTerminal(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        //得到userid和所有terminal的组合
        Map<String, IMUserInfo> userMap = new HashMap<>();
        for (Long userId : userIds) {
            for (Integer terminal : IMTerminalType.codes()) {
                String key = String.join(":", IMRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
                userMap.put(key, new IMUserInfo(userId, terminal));
            }
        }
        List<Object> serverIds = redisMQTemplate.opsForValue().multiGet(userMap.keySet());
        Map<Long, List<IMTerminalType>> userTerminals = new HashMap<>();
        int idx = 0;
        for (Map.Entry<String, IMUserInfo> entry : userMap.entrySet()) {
            if (serverIds.get(idx++) != null) {
                // serverid有值表示用户该terminal在线
                IMUserInfo userInfo = entry.getValue();
                List<IMTerminalType> list = userTerminals.computeIfAbsent(userInfo.getId(), o -> new LinkedList<>());
                list.add(IMTerminalType.fromCode(userInfo.getTerminal()));
            }
        }
        return userTerminals;
    }

    @Override
    public Boolean isOnline(Long userId) {
        String key = String.join(":", IMRedisKey.IM_USER_SERVER_ID, userId.toString(), "*");
        return !Objects.requireNonNull(redisMQTemplate.keys(key)).isEmpty();
    }

    @Override
    public List<Long> getOnlineUser(List<Long> userIds) {
        return new LinkedList<>(getOnlineTerminal(userIds).keySet());
    }

    /**
     * 发送到MQ
     *
     * @param topic    主题
     * @param recvInfo 消息
     */
    public void asyncSendToMQ(String topic, IMRecvInfo recvInfo) {
        String jsonString = JSON.toJSONString(recvInfo);
        rocketMQTemplate.syncSend(topic, jsonString);
//        rocketMQTemplate.asyncSend(topic,jsonString,new SendCallback() {
//
//            @Override
//            public void onSuccess(SendResult sendResult) {
//                log.info("推送到消息队列成功{}",sendResult);
//            }
//
//            @Override
//            public void onException(Throwable throwable) {
//                throw new RuntimeException(throwable);
//            }
//        },5000);
    }
}
