package com.jianhui.project.harbor.common.model;

import lombok.Data;

/**
 * server与前端通信的消息体
 * login,heartbeat,强制下线等
 */
@Data
public class IMSendInfo<T> {

    /**
     * 命令
     */
    private Integer cmd;

    /**
     * 推送消息体
     */
    private T data;

}
