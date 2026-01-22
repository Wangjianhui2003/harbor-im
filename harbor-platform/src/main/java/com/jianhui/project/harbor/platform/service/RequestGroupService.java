package com.jianhui.project.harbor.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.dao.entity.RequestGroupDO;
import com.jianhui.project.harbor.platform.dto.request.AddGroupReqDTO;
import com.jianhui.project.harbor.platform.dto.request.DealGroupReqDTO;

import java.util.List;

/**
 * @author wangj
 */
public interface RequestGroupService extends IService<RequestGroupDO> {

    /**
     * 发送群组加入请求
     * 根据群组的joinType决定：0直接加入，1发送请求，2禁止加入
     *
     * @param dto 请求参数
     */
    void addGroup(AddGroupReqDTO dto);

    /**
     * 处理群组加入请求（同意/拒绝）
     *
     * @param dto 处理参数
     */
    void dealGroupRequest(DealGroupReqDTO dto);

    /**
     * 查询我发送的群组请求
     *
     * @return 请求列表
     */
    List<RequestGroupDO> findSentRequests();

    /**
     * 查询指定群组的加入请求
     *
     * @param groupId 群组ID
     * @return 请求列表
     */
    List<RequestGroupDO> findByGroupId(Long groupId);

    /**
     * 批量查询群组的加入请求
     *
     * @param groupIds 群组ID列表
     * @return 请求列表
     */
    List<RequestGroupDO> findByGroupIds(List<Long> groupIds);
}
