package com.jianhui.project.harbor.platform.enums;

/**
 * 加入类型枚举
 * 用于用户的addType和群聊的joinType
 */
public enum JoinType {
    /**
     * 直接加入/添加
     */
    DIRECT(0),
    /**
     * 需要审批
     */
    APPROVAL(1),
    /**
     * 禁止加入
     */
    FORBIDDEN(2);

    private final Integer code;

    JoinType(Integer code) {
        this.code = code;
    }

    public Integer code() {
        return code;
    }

    public static JoinType fromCode(Integer code) {
        if (code == null) {
            return DIRECT;
        }
        for (JoinType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return DIRECT;
    }
}
