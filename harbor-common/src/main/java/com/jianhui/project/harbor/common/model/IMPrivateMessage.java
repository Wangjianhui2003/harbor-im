package com.jianhui.project.harbor.common.model;

import com.jianhui.project.harbor.common.enums.IMTerminalType;
import lombok.Data;

import java.util.List;


@Data
public class IMPrivateMessage<T> {

    /**
     * 发送方
     */
    private IMUserInfo sender;

    /**
     * 接收者id
     */
    private Long recvId;


    /**
     * 接收者终端类型,默认全部
     */
    private List<Integer> recvTerminals = IMTerminalType.codes();

    /**
     * 是否同步消息给自己的其他终端,默认true
     */
    private Boolean sendToSelf = true;

    /**
     * 是否需要回推发送结果,默认true
     */
    private Boolean isSendBack = true;

    /**
     * 消息内容
     */
    private T data;


}
