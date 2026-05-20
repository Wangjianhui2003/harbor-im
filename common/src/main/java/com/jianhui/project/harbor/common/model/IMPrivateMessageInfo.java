package com.jianhui.project.harbor.common.model;

import lombok.Data;

import java.util.Date;

@Data
public class IMPrivateMessageInfo {

    private Long id;

    private Long sendId;

    private Long recvId;

    private String content;

    private Integer type;

    private Integer status;

    private Date sendTime;
}
