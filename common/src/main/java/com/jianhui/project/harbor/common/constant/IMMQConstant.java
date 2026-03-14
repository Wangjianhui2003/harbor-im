package com.jianhui.project.harbor.common.constant;

/**
 * 消息队列相关常量
 */

public class IMMQConstant {

    /**
     * 私聊消息创建后异步持久化topic
     */
    public static final String PRIVATE_PERSIST_TOPIC = "im_private-message-persist";

    /**
     * 私聊消息持久化消费者组
     */
    public static final String PRIVATE_PERSIST_CONSUMER_GROUP = "im_private-message-persist-consumer-group";

    /**
     * 私聊消息投递消费者组
     */
    public static final String PRIVATE_DISPATCH_CONSUMER_GROUP = "im_private-message-dispatch-consumer-group";

    /**
     * 群聊消息创建后异步持久化topic
     */
    public static final String GROUP_PERSIST_TOPIC = "im_group-message-persist";

    /**
     * 群聊消息持久化消费者组
     */
    public static final String GROUP_PERSIST_CONSUMER_GROUP = "im_group-message-persist-consumer-group";

    /**
     * 群聊消息投递消费者组
     */
    public static final String GROUP_DISPATCH_CONSUMER_GROUP = "im_group-message-dispatch-consumer-group";

    /**
     * 消息topic前缀，后接serverId
     */
    public static final String PRIVATE_MSG_TOPIC_PREFIX = "im_message_private_";

    public static final String GROUP_MSG_TOPIC_PREFIX = "im_message_group_";

    public static final String SYSTEM_MSG_TOPIC_PREFIX = "im_message_system_";


    public static final String PRIVATE_MSG_CONSUMER_GROUP = "im_private-msg-consumer-group_";

    public static final String GROUP_MSG_CONSUMER_GROUP = "im_group-msg-consumer-group_";
}
