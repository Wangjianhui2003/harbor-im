package com.jianhui.project.harbor.platform.service;

import com.jianhui.project.harbor.platform.entity.RequestFriend;

import java.util.List;

/**
 * 好友请求服务接口
 */
public interface RequestFriendService {

    /**
     * 发送添加好友请求
     *
     * @param requestFriend 请求信息
     */
    void addFriendRequest(RequestFriend requestFriend);

    /**
     * 处理添加好友请求
     *
     * @param requestFriend 处理信息
     */
    void dealFriendRequest(RequestFriend requestFriend);

    /**
     * 查询我发送的好友请求列表
     *
     * @return 请求记录列表
     */
    List<RequestFriend> findSentRequests();

    /**
     * 查询我接收的好友请求列表
     *
     * @return 请求记录列表
     */
    List<RequestFriend> findReceivedRequests();
}
