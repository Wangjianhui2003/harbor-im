package com.jianhui.project.harbor.platform.enums;

/**
 * 用户添加类型枚举
 * 对应User表的addType字段
 */
public enum AddType {
    /**
     * 直接添加好友
     */
    DIRECT(0),
    /**
     * 需要同意后添加
     */
    APPROVAL(1),
    /**
     * 禁止添加
     */
    FORBIDDEN(2);

    private final Integer code;

    AddType(Integer code) {
        this.code = code;
    }

    public Integer code() {
        return code;
    }

    public static AddType fromCode(Integer code) {
        if (code == null) {
            return DIRECT;
        }
        for (AddType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return DIRECT;
    }
}
