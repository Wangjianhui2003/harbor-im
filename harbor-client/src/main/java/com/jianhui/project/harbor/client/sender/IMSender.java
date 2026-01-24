package com.jianhui.project.harbor.client.sender;

import com.jianhui.project.harbor.common.enums.IMTerminalType;
import com.jianhui.project.harbor.common.model.IMGroupMessage;
import com.jianhui.project.harbor.common.model.IMPrivateMessage;
import com.jianhui.project.harbor.common.model.IMSystemMessage;

import java.util.List;
import java.util.Map;

/**
 * 发送消息到中间件的接口
 */
public interface IMSender {

    /**
     * 发送系统消息
     */
    <T> void sendSystemMessage(IMSystemMessage<T> message);

    /**
     * 发送私聊消息
     */
    <T> void sendPrivateMessage(IMPrivateMessage<T> message);

    /**
     * 发送群聊消息
     */
    <T> void sendGroupMessage(IMGroupMessage<T> message);

    /**
     * 获取在线的userId和对应的所有终端
     */
    Map<Long, List<IMTerminalType>> getOnlineTerminal(List<Long> userIds);

    /**
     * 该id用户是否在线
     */
    Boolean isOnline(Long userId);

    /**
     * 在这些id里面选出在线的id
     */
    List<Long> getOnlineUser(List<Long> userIds);
}
