package com.jianhui.project.harbor.common.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 群聊消息创建事件
 */
@Data
public class GroupMessageCreatedEvent {

    private Long id;

    private Long groupId;

    private Long sendId;

    private String sendNickname;

    private String content;

    private Integer type;

    private Integer status;

    private Date sendTime;

    private Boolean receipt;

    private Boolean receiptOk;

    private List<Long> atUserIds;

    private List<Long> deliveryRecvIds;

    private Integer senderTerminal;

    private Boolean sendToSelf;

    private Boolean sendBack;
}
