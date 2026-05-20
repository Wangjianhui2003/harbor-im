package com.jianhui.project.harbor.common.model;

import lombok.Data;

@Data
public class IMGroupMessageSendAck {

    private String clientMsgId;

    private Long messageId;

    private Long sendTime;
}
