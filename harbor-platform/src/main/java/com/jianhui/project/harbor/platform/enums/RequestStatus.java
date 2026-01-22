package com.jianhui.project.harbor.platform.enums;

/**
 * 请求状态枚举
 */
public enum RequestStatus {
    /**
     * 待处理
     */
    PENDING(0),
    /**
     * 已同意
     */
    ACCEPTED(1),
    /**
     * 已拒绝
     */
    REJECTED(2);

    private final Integer code;

    RequestStatus(Integer code) {
        this.code = code;
    }

    public Integer code() {
        return code;
    }

    public static RequestStatus fromCode(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (RequestStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
