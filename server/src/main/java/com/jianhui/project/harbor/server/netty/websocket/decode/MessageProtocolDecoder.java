package com.jianhui.project.harbor.server.netty.websocket.decode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jianhui.project.harbor.common.protocol.IMProtoMessageCodec;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.server.constant.ChannelAttrKey;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;

import java.util.List;

public class MessageProtocolDecoder extends MessageToMessageDecoder<WebSocketFrame> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> list) throws Exception {
        AttributeKey<String> protocolAttr = AttributeKey.valueOf(ChannelAttrKey.MESSAGE_PROTOCOL);
        if (frame instanceof TextWebSocketFrame textFrame) {
            ctx.channel().attr(protocolAttr).set(ChannelAttrKey.PROTOCOL_JSON);
            IMSendInfo<?> sendInfo = OBJECT_MAPPER.readValue(textFrame.text(), IMSendInfo.class);
            list.add(sendInfo);
            return;
        }
        if (frame instanceof BinaryWebSocketFrame binaryFrame) {
            ctx.channel().attr(protocolAttr).set(ChannelAttrKey.PROTOCOL_PROTOBUF);
            byte[] payload = new byte[binaryFrame.content().readableBytes()];
            binaryFrame.content().getBytes(binaryFrame.content().readerIndex(), payload);
            list.add(IMProtoMessageCodec.decode(payload));
            return;
        }
        throw new IllegalArgumentException("Unsupported websocket frame: " + frame.getClass().getName());
    }
}
