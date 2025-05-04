package com.jianhui.project.harbor.platform.exception.errorcode;

/**
 * 基础错误码枚举类
 */
public enum BaseErrorCode implements IErrorCode{

    /**
     * 基础错误码
     */
    CLIENT_ERROR("C0000", "客户端异常"),
    SERVER_ERROR("S0000", "服务端异常"),
    REMOTE_ERROR("R0000", "远程调用异常"),
    ;

    private final String code;

    private final String message;

    BaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
