package com.jianhui.project.harbor.common.protocol;

import com.alibaba.fastjson2.JSON;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMGroupMessageDeliveryAck;
import com.jianhui.project.harbor.common.model.IMGroupMessageInfo;
import com.jianhui.project.harbor.common.model.IMGroupMessageSendAck;
import com.jianhui.project.harbor.common.model.IMGroupMessageSendRequest;
import com.jianhui.project.harbor.common.model.IMHeartbeatInfo;
import com.jianhui.project.harbor.common.model.IMLoginInfo;
import com.jianhui.project.harbor.common.model.IMPrivateMessageDeliveryAck;
import com.jianhui.project.harbor.common.model.IMPrivateMessageInfo;
import com.jianhui.project.harbor.common.model.IMPrivateMessageSendAck;
import com.jianhui.project.harbor.common.model.IMPrivateMessageSendRequest;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.common.proto.IMWebSocketProto;

import java.util.Date;
import java.util.Set;

public final class IMProtoMessageCodec {

    private static final Set<IMCmdType> BINARY_COMMANDS = Set.of(
            IMCmdType.LOGIN,
            IMCmdType.HEARTBEAT,
            IMCmdType.FORCE_LOGUT,
            IMCmdType.PRIVATE_MESSAGE,
            IMCmdType.PRIVATE_MESSAGE_SEND,
            IMCmdType.PRIVATE_MESSAGE_SEND_ACK,
            IMCmdType.PRIVATE_MESSAGE_DELIVERY_ACK,
            IMCmdType.GROUP_MESSAGE,
            IMCmdType.GROUP_MESSAGE_SEND,
            IMCmdType.GROUP_MESSAGE_SEND_ACK,
            IMCmdType.GROUP_MESSAGE_DELIVERY_ACK
    );

    private IMProtoMessageCodec() {
    }

    public static boolean supportsBinary(IMSendInfo<?> sendInfo) {
        if (sendInfo == null || sendInfo.getCmd() == null) {
            return false;
        }
        IMCmdType cmdType = IMCmdType.fromCode(sendInfo.getCmd());
        return cmdType != null && BINARY_COMMANDS.contains(cmdType);
    }

    public static IMSendInfo<?> decode(byte[] body) throws InvalidProtocolBufferException {
        IMWebSocketProto.Envelope envelope = IMWebSocketProto.Envelope.parseFrom(body);
        IMCmdType cmdType = IMCmdType.fromCode(envelope.getCmd());
        if (cmdType == null) {
            throw new IllegalArgumentException("Unsupported cmd: " + envelope.getCmd());
        }
        IMSendInfo<Object> sendInfo = new IMSendInfo<>();
        sendInfo.setCmd(cmdType.code());
        sendInfo.setData(decodeData(cmdType, envelope.getPayload()));
        return sendInfo;
    }

    public static byte[] encode(IMSendInfo<?> sendInfo) {
        IMCmdType cmdType = IMCmdType.fromCode(sendInfo.getCmd());
        if (cmdType == null) {
            throw new IllegalArgumentException("Unsupported cmd: " + sendInfo.getCmd());
        }
        byte[] payload = encodeData(cmdType, sendInfo.getData());
        return IMWebSocketProto.Envelope.newBuilder()
                .setCmd(sendInfo.getCmd())
                .setPayload(ByteString.copyFrom(payload))
                .build()
                .toByteArray();
    }

    private static Object decodeData(IMCmdType cmdType, ByteString payload) throws InvalidProtocolBufferException {
        return switch (cmdType) {
            case LOGIN -> {
                IMWebSocketProto.Login login = IMWebSocketProto.Login.parseFrom(payload);
                IMLoginInfo loginInfo = new IMLoginInfo();
                loginInfo.setAccessToken(login.getAccessToken());
                yield loginInfo;
            }
            case HEARTBEAT -> new IMHeartbeatInfo();
            case PRIVATE_MESSAGE_SEND -> {
                IMWebSocketProto.PrivateMessageSendRequest request =
                        IMWebSocketProto.PrivateMessageSendRequest.parseFrom(payload);
                IMPrivateMessageSendRequest data = new IMPrivateMessageSendRequest();
                data.setClientMsgId(request.getClientMsgId());
                data.setRecvId(request.getRecvId());
                data.setContent(request.getContent());
                data.setType(request.getType());
                data.setSendToSelf(request.hasSendToSelf() ? request.getSendToSelf() : Boolean.TRUE);
                yield data;
            }
            case PRIVATE_MESSAGE_DELIVERY_ACK -> {
                IMWebSocketProto.PrivateMessageDeliveryAck ack =
                        IMWebSocketProto.PrivateMessageDeliveryAck.parseFrom(payload);
                IMPrivateMessageDeliveryAck data = new IMPrivateMessageDeliveryAck();
                data.setMessageId(ack.getMessageId());
                yield data;
            }
            case GROUP_MESSAGE_SEND -> {
                IMWebSocketProto.GroupMessageSendRequest request =
                        IMWebSocketProto.GroupMessageSendRequest.parseFrom(payload);
                IMGroupMessageSendRequest data = new IMGroupMessageSendRequest();
                data.setClientMsgId(request.getClientMsgId());
                data.setGroupId(request.getGroupId());
                data.setContent(request.getContent());
                data.setType(request.getType());
                data.setReceipt(request.hasReceipt() ? request.getReceipt() : null);
                data.setAtUserIds(request.getAtUserIdsList());
                data.setSendToSelf(request.hasSendToSelf() ? request.getSendToSelf() : Boolean.TRUE);
                yield data;
            }
            case GROUP_MESSAGE_DELIVERY_ACK -> {
                IMWebSocketProto.GroupMessageDeliveryAck ack =
                        IMWebSocketProto.GroupMessageDeliveryAck.parseFrom(payload);
                IMGroupMessageDeliveryAck data = new IMGroupMessageDeliveryAck();
                data.setMessageId(ack.getMessageId());
                yield data;
            }
            default -> null;
        };
    }

    private static byte[] encodeData(IMCmdType cmdType, Object data) {
        return switch (cmdType) {
            case LOGIN, HEARTBEAT -> IMWebSocketProto.Empty.newBuilder().build().toByteArray();
            case FORCE_LOGUT -> IMWebSocketProto.ForceLogout.newBuilder()
                    .setReason(data == null ? "" : String.valueOf(data))
                    .build()
                    .toByteArray();
            case PRIVATE_MESSAGE -> {
                IMPrivateMessageInfo message = toPrivateMessageInfo(data);
                IMWebSocketProto.PrivateMessage.Builder builder = IMWebSocketProto.PrivateMessage.newBuilder()
                        .setContent(message.getContent() == null ? "" : message.getContent());
                if (message.getId() != null) {
                    builder.setId(message.getId());
                }
                if (message.getSendId() != null) {
                    builder.setSendId(message.getSendId());
                }
                if (message.getRecvId() != null) {
                    builder.setRecvId(message.getRecvId());
                }
                if (message.getType() != null) {
                    builder.setType(message.getType());
                }
                if (message.getStatus() != null) {
                    builder.setStatus(message.getStatus());
                }
                if (message.getSendTime() != null) {
                    builder.setSendTime(message.getSendTime().getTime());
                }
                yield builder.build().toByteArray();
            }
            case PRIVATE_MESSAGE_SEND_ACK -> {
                IMPrivateMessageSendAck ack = toSendAck(data);
                yield IMWebSocketProto.PrivateMessageSendAck.newBuilder()
                        .setClientMsgId(ack.getClientMsgId() == null ? "" : ack.getClientMsgId())
                        .setMessageId(nullToZero(ack.getMessageId()))
                        .setSendTime(nullToZero(ack.getSendTime()))
                        .build()
                        .toByteArray();
            }
            case GROUP_MESSAGE -> {
                IMGroupMessageInfo message = toGroupMessageInfo(data);
                IMWebSocketProto.GroupMessage.Builder builder = IMWebSocketProto.GroupMessage.newBuilder()
                        .setSendNickname(message.getSendNickname() == null ? "" : message.getSendNickname())
                        .setContent(message.getContent() == null ? "" : message.getContent());
                if (message.getId() != null) {
                    builder.setId(message.getId());
                }
                if (message.getGroupId() != null) {
                    builder.setGroupId(message.getGroupId());
                }
                if (message.getSendId() != null) {
                    builder.setSendId(message.getSendId());
                }
                if (message.getType() != null) {
                    builder.setType(message.getType());
                }
                if (message.getReceipt() != null) {
                    builder.setReceipt(message.getReceipt());
                }
                if (message.getReceiptOk() != null) {
                    builder.setReceiptOk(message.getReceiptOk());
                }
                if (message.getReadCount() != null) {
                    builder.setReadCount(message.getReadCount());
                }
                if (message.getAtUserIds() != null) {
                    builder.addAllAtUserIds(message.getAtUserIds());
                }
                if (message.getStatus() != null) {
                    builder.setStatus(message.getStatus());
                }
                if (message.getSendTime() != null) {
                    builder.setSendTime(message.getSendTime().getTime());
                }
                yield builder.build().toByteArray();
            }
            case GROUP_MESSAGE_SEND_ACK -> {
                IMGroupMessageSendAck ack = toGroupSendAck(data);
                yield IMWebSocketProto.GroupMessageSendAck.newBuilder()
                        .setClientMsgId(ack.getClientMsgId() == null ? "" : ack.getClientMsgId())
                        .setMessageId(nullToZero(ack.getMessageId()))
                        .setSendTime(nullToZero(ack.getSendTime()))
                        .build()
                        .toByteArray();
            }
            default -> throw new IllegalArgumentException("Binary encode not supported for cmd: " + cmdType);
        };
    }

    private static IMPrivateMessageInfo toPrivateMessageInfo(Object data) {
        if (data instanceof IMPrivateMessageInfo messageInfo) {
            return messageInfo;
        }
        IMPrivateMessageInfo messageInfo = JSON.parseObject(JSON.toJSONString(data), IMPrivateMessageInfo.class);
        if (messageInfo == null) {
            throw new IllegalArgumentException("Private message payload can not be null");
        }
        if (messageInfo.getSendTime() == null) {
            messageInfo.setSendTime(new Date());
        }
        return messageInfo;
    }

    private static IMPrivateMessageSendAck toSendAck(Object data) {
        if (data instanceof IMPrivateMessageSendAck sendAck) {
            return sendAck;
        }
        IMPrivateMessageSendAck sendAck = JSON.parseObject(JSON.toJSONString(data), IMPrivateMessageSendAck.class);
        if (sendAck == null) {
            throw new IllegalArgumentException("Private message send ack payload can not be null");
        }
        return sendAck;
    }

    private static IMGroupMessageInfo toGroupMessageInfo(Object data) {
        if (data instanceof IMGroupMessageInfo messageInfo) {
            return messageInfo;
        }
        IMGroupMessageInfo messageInfo = JSON.parseObject(JSON.toJSONString(data), IMGroupMessageInfo.class);
        if (messageInfo == null) {
            throw new IllegalArgumentException("Group message payload can not be null");
        }
        if (messageInfo.getReadCount() == null) {
            Long readedCount = JSON.parseObject(JSON.toJSONString(data)).getLong("readedCount");
            if (readedCount != null) {
                messageInfo.setReadCount(readedCount.intValue());
            }
        }
        if (messageInfo.getSendTime() == null) {
            messageInfo.setSendTime(new Date());
        }
        return messageInfo;
    }

    private static IMGroupMessageSendAck toGroupSendAck(Object data) {
        if (data instanceof IMGroupMessageSendAck sendAck) {
            return sendAck;
        }
        IMGroupMessageSendAck sendAck = JSON.parseObject(JSON.toJSONString(data), IMGroupMessageSendAck.class);
        if (sendAck == null) {
            throw new IllegalArgumentException("Group message send ack payload can not be null");
        }
        return sendAck;
    }

    private static Long nullToZero(Number value) {
        return value == null ? 0L : value.longValue();
    }
}
