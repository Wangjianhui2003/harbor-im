package com.jianhui.project.harbor.common.model;

import lombok.Data;

import java.util.List;

@Data
public class IMGroupMessageSendRequest {

    private String clientMsgId;

    private Long groupId;

    private String content;

    private Integer type;

    private Boolean receipt;

    private List<Long> atUserIds;

    private Boolean sendToSelf = true;
}
