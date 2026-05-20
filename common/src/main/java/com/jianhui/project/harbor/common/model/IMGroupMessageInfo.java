package com.jianhui.project.harbor.common.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class IMGroupMessageInfo {

    private Long id;

    private Long groupId;

    private Long sendId;

    private String sendNickname;

    private String content;

    private Integer type;

    private Boolean receipt;

    private Boolean receiptOk;

    private Integer readCount;

    private List<Long> atUserIds;

    private Integer status;

    private Date sendTime;
}
