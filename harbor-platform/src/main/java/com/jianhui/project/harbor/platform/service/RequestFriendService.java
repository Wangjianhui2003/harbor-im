package com.jianhui.project.harbor.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.dao.entity.RequestFriendDO;
import com.jianhui.project.harbor.platform.dto.request.AddFriendReqDTO;
import com.jianhui.project.harbor.platform.dto.request.DealFriendReqDTO;

import java.util.List;

/**
 * @author wangj
 */
public interface RequestFriendService extends IService<RequestFriendDO> {

    /**
     * 发送好友请求
     * 根据对方的addType决定：0直接添加，1发送请求，2禁止添加
     *
     * @param dto 请求参数
     */
    void addFriend(AddFriendReqDTO dto);

    /**
     * 处理好友请求（同意/拒绝）
     *
     * @param dto 处理参数
     */
    void dealFriendRequest(DealFriendReqDTO dto);

    /**
     * 查询我发送的好友请求
     *
     * @return 请求列表
     */
    List<RequestFriendDO> findSentRequests();

    /**
     * 查询我收到的好友请求
     *
     * @return 请求列表
     */
    List<RequestFriendDO> findReceivedRequests();
}
