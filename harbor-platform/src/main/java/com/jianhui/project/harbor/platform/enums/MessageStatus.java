package com.jianhui.project.harbor.platform.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum MessageStatus {

    /**
     * 已保存
     */
    SAVE(0, "已保存"),

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
