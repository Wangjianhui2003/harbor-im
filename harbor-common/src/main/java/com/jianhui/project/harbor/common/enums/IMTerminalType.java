package com.jianhui.project.harbor.common.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public enum IMTerminalType {

    /**
     * web
     */
    WEB(0, "web"),
    /**
     * app
     */
    APP(1, "app"),
    /**
     * pc
     */
    PC(2, "pc"),
    /**
     * 未知
     */
    UNKNOW(-1, "未知");

    private final Integer code;

    private final String desc;


    public static IMTerminalType fromCode(Integer code) {
        for (IMTerminalType typeEnum : values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }

    public static List<Integer> codes() {
        return Arrays.stream(values()).map(IMTerminalType::code).toList();
    }

    public Integer code() {
        return this.code;
    }

}
