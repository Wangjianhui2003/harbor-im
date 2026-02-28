package com.jianhui.project.harbor.server.netty.websocket.decode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class MessageProtocolDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame frame, List<Object> list) throws Exception {
        IMSendInfo sendInfo = OBJECT_MAPPER.readValue(frame.text(), IMSendInfo.class);
        list.add(sendInfo);
    }
}
