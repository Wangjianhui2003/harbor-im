package com.jianhui.project.harbor.platform.constant;

public class RedisKey {
    /**
     * 验证码前缀
     */
    public static final String CHECK_CODE_PREFIX = "easychat:check_code:";

    /**
     * 验证码过期时间
     */
    public static final long CHECK_CODE_EXPIRE_TIME = 5 * 60;

    /**
     *  前缀，后接用户id，redis里有值说明该用户忙线
     */
    public static final String IM_USER_BUSY = "im:user:busy";
    /**
     * 已读群聊消息位置(已读最大id)
     */
    public static final String IM_GROUP_READED_POSITION = "im:readed:group:position";
    /**
     * webrtc 单人通话
     */
    public static final String IM_WEBRTC_PRIVATE_SESSION = "im:webrtc:private:session";
    /**
     * webrtc 群通话
     */
    public static final String IM_WEBRTC_GROUP_SESSION = "im:webrtc:group:session";

    /**
     * 用户被封禁消息队列
     */
    public static final String IM_QUEUE_USER_BANNED = "im:queue:user:banned";

    /**
     * 群聊被封禁消息队列
     */
    public static final String IM_QUEUE_GROUP_BANNED = "im:queue:group:banned";

    /**
     * 群聊解封消息队列
     */
    public static final String IM_QUEUE_GROUP_UNBAN = "im:queue:group:unban";

    /**
     * 缓存是否好友：bool
     */
    public static final String IM_CACHE_FRIEND = "im:cache:friend";
    /**
     * 缓存群聊信息
     */
    public static final String IM_CACHE_GROUP =  "im:cache:group";
    /**
     * 缓存群聊成员id
     */
    public static final String IM_CACHE_GROUP_MEMBER_ID = "im:cache:group_member_ids";

    /**
     * 重复提交
     */
    public static final String IM_REPEAT_SUBMIT = "im:repeat:submit";
}
