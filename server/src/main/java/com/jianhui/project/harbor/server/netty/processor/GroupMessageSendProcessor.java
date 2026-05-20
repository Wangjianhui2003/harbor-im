package com.jianhui.project.harbor.server.netty.processor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.GroupMessageCreatedEvent;
import com.jianhui.project.harbor.common.model.IMGroupMessageSendAck;
import com.jianhui.project.harbor.common.model.IMGroupMessageSendRequest;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.server.config.props.GroupMessageMQProperties;
import com.jianhui.project.harbor.server.constant.ChannelAttrKey;
import com.jianhui.project.harbor.server.model.GroupMessageSendDedupRecord;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupMessageSendProcessor extends AbstractMsgProcessor<IMGroupMessageSendRequest> {

    private static final int SAVED_STATUS = 0;

    private static final String SEND_DEDUP_KEY_PREFIX = "im:group:send:dedup";

    @Qualifier("groupMessageSendDedupCache")
    private final Cache<String, GroupMessageSendDedupRecord> groupMessageSendDedupCache;

    private final RocketMQTemplate rocketMQTemplate;
    private final GroupMessageMQProperties mqProperties;

    @Override
    public IMCmdType getCmdType() {
        return IMCmdType.GROUP_MESSAGE_SEND;
    }

    @Override
    public void process(ChannelHandlerContext ctx, IMGroupMessageSendRequest request) {
        if (request == null || request.getGroupId() == null || request.getType() == null
                || StringUtils.isBlank(request.getContent()) || StringUtils.isBlank(request.getClientMsgId())) {
            log.warn("非法群聊发送请求, channel:{}", ctx.channel().id().asLongText());
            return;
        }
        Long userId = ctx.channel().attr(AttributeKey.<Long>valueOf(ChannelAttrKey.USER_ID)).get();
        Integer terminal = ctx.channel().attr(AttributeKey.<Integer>valueOf(ChannelAttrKey.TERMINAL_TYPE)).get();
        if (userId == null || terminal == null) {
            log.warn("用户尚未登录，忽略群聊发送请求, channel:{}", ctx.channel().id().asLongText());
            ctx.channel().close();
            return;
        }
        String dedupKey = buildSendDedupKey(userId, terminal, request.getClientMsgId());
        GroupMessageSendDedupRecord existing = groupMessageSendDedupCache.getIfPresent(dedupKey);
        if (existing != null) {
            if (existing.isAcked()) {
                sendAck(ctx, existing.toAck());
            } else {
                log.debug("命中群聊发送去重缓存，等待首次请求异步回调, userId:{}, terminal:{}, clientMsgId:{}",
                        userId, terminal, request.getClientMsgId());
            }
            return;
        }

        Long messageId = IdUtil.getSnowflakeNextId();
        Date sendTime = new Date();
        GroupMessageSendDedupRecord record =
                new GroupMessageSendDedupRecord(request.getClientMsgId(), messageId, sendTime.getTime());
        GroupMessageSendDedupRecord previous = groupMessageSendDedupCache.asMap().putIfAbsent(dedupKey, record);
        if (previous != null) {
            if (previous.isAcked()) {
                sendAck(ctx, previous.toAck());
            }
            return;
        }

        GroupMessageCreatedEvent event = new GroupMessageCreatedEvent();
        event.setId(messageId);
        event.setClientMsgId(request.getClientMsgId());
        event.setGroupId(request.getGroupId());
        event.setSendId(userId);
        event.setContent(request.getContent());
        event.setType(request.getType());
        event.setStatus(SAVED_STATUS);
        event.setSendTime(sendTime);
        event.setReceipt(request.getReceipt());
        event.setReceiptOk(null);
        event.setAtUserIds(request.getAtUserIds());
        event.setSenderTerminal(terminal);
        event.setSendToSelf(!Boolean.FALSE.equals(request.getSendToSelf()));
        event.setSendBack(true);

        String payload = JSON.toJSONString(event);
        try {
            rocketMQTemplate.asyncSend(IMMQConstant.GROUP_BUS_TOPIC, payload, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    record.markAcked();
                    sendAck(ctx, record.toAck());
                    log.debug("群聊消息已异步投递到总线topic, msgId:{}, userId:{}, groupId:{}, rocketMsgId:{}",
                            messageId, userId, request.getGroupId(), sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    groupMessageSendDedupCache.asMap().remove(dedupKey, record);
                    log.error("群聊消息异步投递总线topic失败, msgId:{}, userId:{}, groupId:{}",
                            messageId, userId, request.getGroupId(), e);
                }
            }, mqProperties.getProducer().getSendTimeoutMs());
        } catch (Exception e) {
            groupMessageSendDedupCache.asMap().remove(dedupKey, record);
            log.error("群聊消息异步投递总线topic异常, msgId:{}, userId:{}, groupId:{}",
                    messageId, userId, request.getGroupId(), e);
        }
    }

    @Override
    public IMGroupMessageSendRequest transForm(Object o) {
        if (o instanceof IMGroupMessageSendRequest request) {
            return request;
        }
        HashMap map = (HashMap) o;
        return BeanUtil.fillBeanWithMap(map, new IMGroupMessageSendRequest(), false);
    }

    private void sendAck(ChannelHandlerContext ctx, IMGroupMessageSendAck ack) {
        if (!ctx.channel().isActive()) {
            return;
        }
        IMSendInfo<IMGroupMessageSendAck> sendInfo = new IMSendInfo<>();
        sendInfo.setCmd(IMCmdType.GROUP_MESSAGE_SEND_ACK.code());
        sendInfo.setData(ack);
        ctx.channel().writeAndFlush(sendInfo);
    }

    private String buildSendDedupKey(Long userId, Integer terminal, String clientMsgId) {
        return String.join(":", SEND_DEDUP_KEY_PREFIX, userId.toString(), terminal.toString(), clientMsgId);
    }
}
