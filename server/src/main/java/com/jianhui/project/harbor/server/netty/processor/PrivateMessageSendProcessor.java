package com.jianhui.project.harbor.server.netty.processor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMPrivateMessageSendAck;
import com.jianhui.project.harbor.common.model.IMPrivateMessageSendRequest;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.common.model.PrivateMessageCreatedEvent;
import com.jianhui.project.harbor.server.config.props.PrivateMessageMQProperties;
import com.jianhui.project.harbor.server.constant.ChannelAttrKey;
import com.jianhui.project.harbor.server.model.PrivateMessageSendDedupRecord;
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
public class PrivateMessageSendProcessor extends AbstractMsgProcessor<IMPrivateMessageSendRequest> {

    private static final int SAVED_STATUS = 0;

    private static final String SEND_DEDUP_KEY_PREFIX = "im:private:send:dedup";

    @Qualifier("privateMessageSendDedupCache")
    private final Cache<String, PrivateMessageSendDedupRecord> privateMessageSendDedupCache;

    private final RocketMQTemplate rocketMQTemplate;
    private final PrivateMessageMQProperties mqProperties;

    @Override
    public IMCmdType getCmdType() {
        return IMCmdType.PRIVATE_MESSAGE_SEND;
    }

    @Override
    public void process(ChannelHandlerContext ctx, IMPrivateMessageSendRequest request) {
        if (request == null || request.getRecvId() == null || request.getType() == null
                || StringUtils.isBlank(request.getContent()) || StringUtils.isBlank(request.getClientMsgId())) {
            log.warn("非法私聊发送请求, channel:{}", ctx.channel().id().asLongText());
            return;
        }
        Long userId = ctx.channel().attr(AttributeKey.<Long>valueOf(ChannelAttrKey.USER_ID)).get();
        Integer terminal = ctx.channel().attr(AttributeKey.<Integer>valueOf(ChannelAttrKey.TERMINAL_TYPE)).get();
        if (userId == null || terminal == null) {
            log.warn("用户尚未登录，忽略私聊发送请求, channel:{}", ctx.channel().id().asLongText());
            ctx.channel().close();
            return;
        }
        String dedupKey = buildSendDedupKey(userId, terminal, request.getClientMsgId());
        PrivateMessageSendDedupRecord existing = privateMessageSendDedupCache.getIfPresent(dedupKey);
        if (existing != null) {
            if (existing.isAcked()) {
                sendAck(ctx, existing.toAck());
            } else {
                log.debug("命中私聊发送去重缓存，等待首次请求异步回调, userId:{}, terminal:{}, clientMsgId:{}",
                        userId, terminal, request.getClientMsgId());
            }
            return;
        }

        Long messageId = IdUtil.getSnowflakeNextId();
        Date sendTime = new Date();
        PrivateMessageSendDedupRecord record =
                new PrivateMessageSendDedupRecord(request.getClientMsgId(), messageId, sendTime.getTime());
        PrivateMessageSendDedupRecord previous = privateMessageSendDedupCache.asMap().putIfAbsent(dedupKey, record);
        if (previous != null) {
            if (previous.isAcked()) {
                sendAck(ctx, previous.toAck());
            }
            return;
        }

        PrivateMessageCreatedEvent event = new PrivateMessageCreatedEvent();
        event.setId(messageId);
        event.setClientMsgId(request.getClientMsgId());
        event.setSendId(userId);
        event.setRecvId(request.getRecvId());
        event.setContent(request.getContent());
        event.setType(request.getType());
        event.setStatus(SAVED_STATUS);
        event.setSendTime(sendTime);
        event.setSenderTerminal(terminal);
        event.setSendToSelf(!Boolean.FALSE.equals(request.getSendToSelf()));
        event.setSendBack(true);

        String payload = JSON.toJSONString(event);
        try {
            rocketMQTemplate.asyncSend(IMMQConstant.PRIVATE_BUS_TOPIC, payload, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    record.markAcked();
                    sendAck(ctx, record.toAck());
                    log.debug("私聊消息已异步投递到总线topic, msgId:{}, userId:{}, recvId:{}, rocketMsgId:{}",
                            messageId, userId, request.getRecvId(), sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    privateMessageSendDedupCache.asMap().remove(dedupKey, record);
                    log.error("私聊消息异步投递总线topic失败, msgId:{}, userId:{}, recvId:{}",
                            messageId, userId, request.getRecvId(), e);
                }
            }, mqProperties.getProducer().getSendTimeoutMs());
        } catch (Exception e) {
            privateMessageSendDedupCache.asMap().remove(dedupKey, record);
            log.error("私聊消息异步投递总线topic异常, msgId:{}, userId:{}, recvId:{}",
                    messageId, userId, request.getRecvId(), e);
        }
    }

    @Override
    public IMPrivateMessageSendRequest transForm(Object o) {
        if (o instanceof IMPrivateMessageSendRequest request) {
            return request;
        }
        HashMap map = (HashMap) o;
        return BeanUtil.fillBeanWithMap(map, new IMPrivateMessageSendRequest(), false);
    }

    private void sendAck(ChannelHandlerContext ctx, IMPrivateMessageSendAck ack) {
        if (!ctx.channel().isActive()) {
            return;
        }
        IMSendInfo<IMPrivateMessageSendAck> sendInfo = new IMSendInfo<>();
        sendInfo.setCmd(IMCmdType.PRIVATE_MESSAGE_SEND_ACK.code());
        sendInfo.setData(ack);
        ctx.channel().writeAndFlush(sendInfo);
    }

    private String buildSendDedupKey(Long userId, Integer terminal, String clientMsgId) {
        return String.join(":", SEND_DEDUP_KEY_PREFIX, userId.toString(), terminal.toString(), clientMsgId);
    }
}
