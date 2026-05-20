package com.jianhui.project.harbor.server.model;

import com.jianhui.project.harbor.common.model.IMGroupMessageSendAck;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GroupMessageSendDedupRecord {

    private final String clientMsgId;

    private final Long messageId;

    private final Long sendTime;

    private volatile boolean acked;

    public IMGroupMessageSendAck toAck() {
        IMGroupMessageSendAck ack = new IMGroupMessageSendAck();
        ack.setClientMsgId(clientMsgId);
        ack.setMessageId(messageId);
        ack.setSendTime(sendTime);
        return ack;
    }

    public void markAcked() {
        this.acked = true;
    }
}
