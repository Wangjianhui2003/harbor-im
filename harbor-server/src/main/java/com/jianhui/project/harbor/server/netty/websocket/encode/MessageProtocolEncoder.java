package com.jianhui.project.harbor.server.netty.websocket.encode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class MessageProtocolEncoder extends MessageToMessageEncoder<IMSendInfo> {
    @Override
    protected void encode(ChannelHandlerContext ctx, IMSendInfo sendInfo, List<Object> list) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String s = objectMapper.writeValueAsString(sendInfo);
        TextWebSocketFrame frame = new TextWebSocketFrame(s);
        list.add(frame);
    }
}
