package com.jianhui.project.harbor.server.model;

import com.jianhui.project.harbor.common.model.IMPrivateMessageSendAck;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PrivateMessageSendDedupRecord {

    private final String clientMsgId;

    private final Long messageId;

    private final Long sendTime;

    private volatile boolean acked;

    public IMPrivateMessageSendAck toAck() {
        IMPrivateMessageSendAck ack = new IMPrivateMessageSendAck();
        ack.setClientMsgId(clientMsgId);
        ack.setMessageId(messageId);
        ack.setSendTime(sendTime);
        return ack;
    }

    public void markAcked() {
        this.acked = true;
    }
}
