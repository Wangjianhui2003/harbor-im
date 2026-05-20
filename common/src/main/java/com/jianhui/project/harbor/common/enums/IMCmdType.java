package com.jianhui.project.harbor.common.enums;


import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum IMCmdType {

    /**
     * 登录
     */
    LOGIN(0, "登录"),

    /**
     * 心跳
     */
    HEARTBEAT(1, "心跳"),

    /**
     * 强制下线
     */
    FORCE_LOGUT(2, "强制下线"),

    /**
     * 私聊消息
     */
    PRIVATE_MESSAGE(3, "私聊消息"),

    /**
     * 群发消息
     */
    GROUP_MESSAGE(4, "群发消息"),

    /**
     * 系统消息
     */
    SYSTEM_MESSAGE(5, "系统消息"),

    /**
     * 私聊消息发送请求
     */
    PRIVATE_MESSAGE_SEND(6, "私聊消息发送请求"),

    /**
     * 私聊消息发送确认
     */
    PRIVATE_MESSAGE_SEND_ACK(7, "私聊消息发送确认"),

    /**
     * 私聊消息投递确认
     */
    PRIVATE_MESSAGE_DELIVERY_ACK(8, "私聊消息投递确认"),

    /**
     * 群聊消息发送请求
     */
    GROUP_MESSAGE_SEND(9, "群聊消息发送请求"),

    /**
     * 群聊消息发送确认
     */
    GROUP_MESSAGE_SEND_ACK(10, "群聊消息发送确认"),

    /**
     * 群聊消息投递确认
     */
    GROUP_MESSAGE_DELIVERY_ACK(11, "群聊消息投递确认");


    private final Integer code;

    private final String desc;

    public static IMCmdType fromCode(Integer code) {
        for (IMCmdType typeEnum : values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }


    public Integer code() {
        return this.code;
    }


}
