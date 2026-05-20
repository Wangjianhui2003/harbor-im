package com.jianhui.project.harbor.server.netty.websocket.encode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.common.protocol.IMProtoMessageCodec;
import com.jianhui.project.harbor.server.constant.ChannelAttrKey;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;

import java.util.List;

public class MessageProtocolEncoder extends MessageToMessageEncoder<IMSendInfo> {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("LongToString");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);
        return objectMapper;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, IMSendInfo sendInfo, List<Object> list) throws Exception {
        AttributeKey<String> protocolAttr = AttributeKey.valueOf(ChannelAttrKey.MESSAGE_PROTOCOL);
        String protocol = ctx.channel().attr(protocolAttr).get();
        if (ChannelAttrKey.PROTOCOL_PROTOBUF.equals(protocol) && IMProtoMessageCodec.supportsBinary(sendInfo)) {
            byte[] payload = IMProtoMessageCodec.encode(sendInfo);
            list.add(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(payload)));
            return;
        }
        String s = OBJECT_MAPPER.writeValueAsString(sendInfo);
        TextWebSocketFrame frame = new TextWebSocketFrame(s);
        list.add(frame);
    }
}
