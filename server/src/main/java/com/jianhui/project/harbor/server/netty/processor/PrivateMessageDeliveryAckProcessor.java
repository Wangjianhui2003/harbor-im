package com.jianhui.project.harbor.server.netty.processor;

import cn.hutool.core.bean.BeanUtil;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMPrivateMessageDeliveryAck;
import com.jianhui.project.harbor.server.constant.ChannelAttrKey;
import com.jianhui.project.harbor.server.service.PrivateMessageDeliveryService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMessageDeliveryAckProcessor extends AbstractMsgProcessor<IMPrivateMessageDeliveryAck> {

    private final PrivateMessageDeliveryService privateMessageDeliveryService;

    @Override
    public IMCmdType getCmdType() {
        return IMCmdType.PRIVATE_MESSAGE_DELIVERY_ACK;
    }

    @Override
    public void process(ChannelHandlerContext ctx, IMPrivateMessageDeliveryAck ack) {
        Long userId = ctx.channel().attr(AttributeKey.<Long>valueOf(ChannelAttrKey.USER_ID)).get();
        Integer terminal = ctx.channel().attr(AttributeKey.<Integer>valueOf(ChannelAttrKey.TERMINAL_TYPE)).get();
        if (ack == null || ack.getMessageId() == null || userId == null || terminal == null) {
            log.warn("收到非法私聊投递ack, channel:{}", ctx.channel().id().asLongText());
            return;
        }
        privateMessageDeliveryService.acknowledge(ack.getMessageId(), userId, terminal);
    }

    @Override
    public IMPrivateMessageDeliveryAck transForm(Object o) {
        if (o instanceof IMPrivateMessageDeliveryAck ack) {
            return ack;
        }
        HashMap map = (HashMap) o;
        return BeanUtil.fillBeanWithMap(map, new IMPrivateMessageDeliveryAck(), false);
    }
}
