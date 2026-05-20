package com.jianhui.project.harbor.server.constant;

public final class ChannelAttrKey {

    private ChannelAttrKey() {
    }

    /**
     * 用户ID
     */
    public static final String USER_ID = "USER_ID";
    /**
     * 终端类型
     */
    public static final String TERMINAL_TYPE = "TERMINAL_TYPE";
    /**
     * 心跳次数
     */
    public static final String HEARTBEAT_TIMES = "HEARTBEAT_TIMES";

    /**
     * websocket 消息协议
     */
    public static final String MESSAGE_PROTOCOL = "MESSAGE_PROTOCOL";

    public static final String PROTOCOL_JSON = "json";

    public static final String PROTOCOL_PROTOBUF = "protobuf";

}
