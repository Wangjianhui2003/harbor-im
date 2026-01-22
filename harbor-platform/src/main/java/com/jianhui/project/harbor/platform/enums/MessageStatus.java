package com.jianhui.project.harbor.platform.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum MessageStatus {

    /**
     * 文件
     */
    UNSENT(0, "未送达"),

    /**
     * 已发送`
     */
    SENT(1, "送达"),

    /**
     * 撤回
     */
    RECALL(2, "撤回"),

    /**
     * 已读
     */
    READ(3, "已读");

    private final Integer code;

    private final String desc;


    public Integer code() {
        return this.code;
    }
}
