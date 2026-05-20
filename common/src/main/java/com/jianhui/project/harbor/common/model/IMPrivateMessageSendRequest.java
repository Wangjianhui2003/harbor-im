package com.jianhui.project.harbor.common.model;

import lombok.Data;

@Data
public class IMPrivateMessageSendRequest {

    private String clientMsgId;

    private Long recvId;

    private String content;

    private Integer type;

    private Boolean sendToSelf = true;
}
