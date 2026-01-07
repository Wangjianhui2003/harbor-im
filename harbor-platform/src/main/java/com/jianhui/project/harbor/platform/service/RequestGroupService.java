package com.jianhui.project.harbor.platform.service;

import com.jianhui.project.harbor.platform.entity.RequestGroup;

import java.util.List;

/**
 * 群组请求服务接口
 */
public interface RequestGroupService {

    /**
     * 发送添加群组请求
     *
     * @param requestGroup 请求信息
     */
    void addGroupRequest(RequestGroup requestGroup);

    /**
     * 处理添加群组请求
     *
     * @param requestGroup 处理信息
     */
    void dealGroupRequest(RequestGroup requestGroup);

    /**
     * 查询我发送的群组请求列表
     *
     * @return 请求记录列表
     */
    List<RequestGroup> findSentRequests();

    /**
     * 查询指定群组的请求列表（用于群主/管理员查看）
     *
     * @param groupId 群组id
     * @return 请求记录列表
     */
    List<RequestGroup> findGroupRequests(Long groupId);
}
