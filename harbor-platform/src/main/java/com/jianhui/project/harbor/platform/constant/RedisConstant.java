package com.jianhui.project.harbor.platform.constant;

public class RedisConstant {
    /**
     * 验证码前缀
     */
    public static final String CHECK_CODE_PREFIX = "easychat:check_code:";

    /**
     * 验证码过期时间
     */
    public static final long CHECK_CODE_EXPIRE_TIME = 5 * 60;

    /**
     * 用户信息前缀
     */
    public static final String USER_WS_HEART_BEAT_PREFIX = "easychat:user_heart_beat:";

    /**
     * 用户登录返回实体的Key前缀
     * token -> tokenDTO
     */
    public static final String USER_LOGIN_TOKENDTO_PREFIX = "easychat:user_login_tokenDTO:";

    /**
     * 用户登录token的Key前缀
     * user_id -> token
     */
    public static final String USER_LOGIN_TOKEN_PREFIX = "easychat:user_login_token:";

    /**
     * token过期时间
     */
    public static final int USER_TOKEN_EXPIRE_TIME = 60 * 60 * 24;
}
