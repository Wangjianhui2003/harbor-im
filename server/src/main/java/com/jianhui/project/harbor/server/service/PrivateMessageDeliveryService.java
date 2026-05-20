package com.jianhui.project.harbor.server.service;

import com.alibaba.fastjson2.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMPrivateMessageInfo;
import com.jianhui.project.harbor.common.model.IMRecvInfo;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.server.config.props.PrivateMessageMQProperties;
import com.jianhui.project.harbor.server.netty.UserChannelCxtMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrivateMessageDeliveryService {

    private static final String DELIVERY_KEY_PREFIX = "im:private:delivery:ack";

    @Qualifier("privateMessageDeliveryCache")
    private final Cache<String, Boolean> privateMessageDeliveryCache;

    private final PrivateMessageMQProperties mqProperties;

    private final ConcurrentMap<String, CompletableFuture<Void>> pendingAckMap = new ConcurrentHashMap<>();

    public void deliverAndAwaitAck(IMRecvInfo recvInfo) {
        IMPrivateMessageInfo messageInfo = normalize(recvInfo);
        Long messageId = messageInfo == null ? null : messageInfo.getId();
        List<PendingDelivery> pendingDeliveries = new ArrayList<>();
        for (IMUserInfo receiver : recvInfo.getReceivers()) {
            ChannelHandlerContext channelCtx = UserChannelCxtMap.getChannelCtx(receiver.getId(), receiver.getTerminal());
            if (channelCtx == null) {
                if (messageId == null) {
                    log.warn("未找到channel，跳过无需ack的私聊消息投递，接收者:{},终端:{}", receiver.getId(), receiver.getTerminal());
                    continue;
                }
                throw new IllegalStateException(String.format("未找到channel，messageId:%s，receiver:%s，terminal:%s",
                        messageId, receiver.getId(), receiver.getTerminal()));
            }
            IMSendInfo<Object> sendInfo = new IMSendInfo<>();
            sendInfo.setCmd(IMCmdType.PRIVATE_MESSAGE.code());
            sendInfo.setData(messageInfo != null ? messageInfo : recvInfo.getData());
            if (messageId == null) {
                channelCtx.channel().writeAndFlush(sendInfo);
                continue;
            }
            String deliveryKey = buildDeliveryKey(messageId, receiver.getId(), receiver.getTerminal());
            if (Boolean.TRUE.equals(privateMessageDeliveryCache.getIfPresent(deliveryKey))) {
                log.debug("跳过已确认的重复私聊消息消费，messageId:{}, receiver:{}, terminal:{}",
                        messageId, receiver.getId(), receiver.getTerminal());
                continue;
            }
            CompletableFuture<Void> future = new CompletableFuture<>();
            CompletableFuture<Void> currentFuture = pendingAckMap.putIfAbsent(deliveryKey, future);
            if (currentFuture == null) {
                currentFuture = future;
                channelCtx.channel().writeAndFlush(sendInfo).addListener((ChannelFutureListener) channelFuture -> {
                    if (!channelFuture.isSuccess()) {
                        pendingAckMap.remove(deliveryKey, future);
                        future.completeExceptionally(channelFuture.cause());
                    }
                });
            }
            pendingDeliveries.add(new PendingDelivery(deliveryKey, currentFuture));
        }
        waitForAck(pendingDeliveries);
    }

    public void acknowledge(Long messageId, Long userId, Integer terminal) {
        if (messageId == null || userId == null || terminal == null) {
            return;
        }
        String key = buildDeliveryKey(messageId, userId, terminal);
        privateMessageDeliveryCache.put(key, Boolean.TRUE);
        CompletableFuture<Void> future = pendingAckMap.remove(key);
        if (future != null) {
            future.complete(null);
        }
    }

    private void waitForAck(List<PendingDelivery> pendingDeliveries) {
        if (pendingDeliveries.isEmpty()) {
            return;
        }
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(mqProperties.getDelivery().getAckTimeoutMs());
        for (PendingDelivery pendingDelivery : pendingDeliveries) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                cleanupPending(pendingDeliveries);
                throw new IllegalStateException("等待私聊消息ack超时");
            }
            try {
                pendingDelivery.future().get(remainingNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cleanupPending(pendingDeliveries);
                throw new IllegalStateException("等待私聊消息ack被中断", e);
            } catch (ExecutionException | TimeoutException e) {
                cleanupPending(pendingDeliveries);
                throw new IllegalStateException("等待私聊消息ack失败", e);
            }
        }
    }

    private void cleanupPending(List<PendingDelivery> pendingDeliveries) {
        for (PendingDelivery pendingDelivery : pendingDeliveries) {
            pendingAckMap.remove(pendingDelivery.key(), pendingDelivery.future());
        }
    }

    private IMPrivateMessageInfo normalize(IMRecvInfo recvInfo) {
        if (recvInfo.getData() == null) {
            return null;
        }
        if (recvInfo.getData() instanceof IMPrivateMessageInfo messageInfo) {
            return messageInfo;
        }
        IMPrivateMessageInfo messageInfo = JSON.parseObject(JSON.toJSONString(recvInfo.getData()), IMPrivateMessageInfo.class);
        recvInfo.setData(messageInfo);
        return messageInfo;
    }

    private String buildDeliveryKey(Long messageId, Long userId, Integer terminal) {
        return String.join(":",
                DELIVERY_KEY_PREFIX,
                messageId.toString(),
                userId.toString(),
                terminal.toString());
    }

    private record PendingDelivery(String key, CompletableFuture<Void> future) {
    }
}
