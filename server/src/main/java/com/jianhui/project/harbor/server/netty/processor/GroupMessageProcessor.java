package com.jianhui.project.harbor.server.netty.processor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMRecvInfo;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.server.netty.UserChannelCxtMap;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 群聊消息处理器
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GroupMessageProcessor extends AbstractMsgProcessor<IMRecvInfo> {

    private static final String DELIVERY_DEDUP_KEY_PREFIX = "im:group:delivery:dedup";

    @Qualifier("groupMessageDeliveryCache")
    private final Cache<String, Boolean> groupMessageDeliveryCache;

    @Override
    public IMCmdType getCmdType() {
        return IMCmdType.GROUP_MESSAGE;
    }

    @Override
    public void process(IMRecvInfo recvInfo) {
        IMUserInfo sender = recvInfo.getSender();
        Long messageId = extractMessageId(recvInfo.getData());
        log.info("接收到群消息，发送者:{},接收用户数量:{}，内容:{}",
                sender.getId(), recvInfo.getReceivers().size(), recvInfo.getData());
        for (IMUserInfo receiver : recvInfo.getReceivers()) {
            try {
                ChannelHandlerContext channelCtx = UserChannelCxtMap.getChannelCtx(receiver.getId(), receiver.getTerminal());
                if (channelCtx != null) {
                    if (isDuplicateDelivery(messageId, receiver)) {
                        log.debug("跳过重复群聊投递，消息id:{},接收者:{},终端:{}",
                                messageId, receiver.getId(), receiver.getTerminal());
                        continue;
                    }
                    IMSendInfo<Object> sendInfo = new IMSendInfo<>();
                    sendInfo.setCmd(IMCmdType.GROUP_MESSAGE.code());
                    sendInfo.setData(recvInfo.getData());
                    channelCtx.writeAndFlush(sendInfo);
                } else {
                    log.warn("未找到接收者channel,接收者:{},内容:{}", receiver.getId(), recvInfo.getData());
                }
            } catch (Exception e) {
                log.error("发送异常，,接收者:{}，内容:{}", receiver.getId(), recvInfo.getData(), e);
            }
        }
    }

    private boolean isDuplicateDelivery(Long messageId, IMUserInfo receiver) {
        if (messageId == null) {
            return false;
        }
        String key = String.join(":",
                DELIVERY_DEDUP_KEY_PREFIX,
                messageId.toString(),
                receiver.getId().toString(),
                receiver.getTerminal().toString());
        Boolean firstDelivery = groupMessageDeliveryCache.asMap().putIfAbsent(key, Boolean.TRUE);
        return firstDelivery != null;
    }

    private Long extractMessageId(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof JSONObject jsonObject) {
            return jsonObject.getLong("id");
        }
        try {
            Object json = JSON.toJSON(data);
            if (json instanceof JSONObject jsonObject) {
                return jsonObject.getLong("id");
            }
        } catch (Exception e) {
            log.debug("提取群聊消息id失败，data:{}", data, e);
        }
        return null;
    }
}
