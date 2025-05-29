package com.jianhui.project.harbor.platform.enums;

/**
 * 群身份枚举
 */
public enum GroupRole {
    OWNER(0),
    ADMIN(1),
    MEMBER(2);

    Integer code;

    GroupRole(Integer code) {
        this.code = code;
    }
    
    public Integer code(){
        return code;
    }
}

