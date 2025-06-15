package com.jianhui.project.harbor.platform.session;

import lombok.Data;


/**
 * 私聊RTC会话信息
 */
@Data
public class WebRTCPrivateSession {

    /**
     * 发起者id
     */
    private Long callerId;

    /**
     * 发起者终端类型
     */
    private Integer callerTerminal;

    /**
     * 接受者id
     */
    private Long acceptorId;

    /**
     * 接受者终端类型
     */
    private Integer acceptorTerminal;

    /**
     *  通话模式
     */
    private String mode;

    /**
     * 开始聊天时间戳
     */
    private Long  chatTimeStamp;
}
