package com.jianhui.project.harbor.common.model;

import lombok.Data;

import java.util.Date;

/**
 * 私聊消息创建事件
 */
@Data
public class PrivateMessageCreatedEvent {

    private Long id;

    private Long sendId;

    private Long recvId;

    private String content;

    private Integer type;

    private Integer status;

    private Date sendTime;

    private Integer senderTerminal;

    private Boolean sendToSelf;

    private Boolean sendBack;
}
