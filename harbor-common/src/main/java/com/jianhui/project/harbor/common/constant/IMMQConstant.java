package com.jianhui.project.harbor.common.constant;

/**
 * 消息队列相关常量
 */

public class IMMQConstant {

    /**
     * 消息topic前缀，后接serverId
     */
    public static final String PRIVATE_MSG_TOPIC_PREFIX = "im_message_private_";

    public static final String GROUP_MSG_TOPIC_PREFIX = "im_message_group_";

    public static final String SYSTEM_MSG_TOPIC_PREFIX = "im_message_system_";


    /**
     * 发送结果topic，后接appName
     */
    public static final String PRIVATE_RESULT_TOPIC_PREFIX = "im_result_private_";

    public static final String GROUP_RESULT_TOPIC_PREFIX = "im_result_group_";

    public static final String SYSTEM_RESULT_TOPIC_PREFIX = "im_result_system_";

    /**
     * 消息发送结果消费者组,后接appName
     */
    public static final String GROUP_RESULT_MSG_CONSUMER_PREFIX = "im_group-msg-result-consumer-group_";

    public static final String PRIVATE_RESULT_MSG_CONSUMER_PREFIX = "im_private-msg-result-consumer-group_";

    public static final String PRIVATE_MSG_CONSUMER_GROUP = "im_private-msg-consumer-group_";

    public static final String GROUP_MSG_CONSUMER_GROUP = "im_group-msg-consumer-group_";
}
