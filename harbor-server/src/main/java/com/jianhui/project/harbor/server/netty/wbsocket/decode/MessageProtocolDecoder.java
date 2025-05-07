package com.jianhui.project.harbor.server.netty.wbsocket.decode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class MessageProtocolDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {
    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame frame, List<Object> list) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        IMSendInfo sendInfo = objectMapper.readValue(frame.text(), IMSendInfo.class);
        list.add(sendInfo);
    }
}
