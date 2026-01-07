package com.jianhui.project.harbor.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jianhui.project.harbor.platform.entity.RequestGroup;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

public interface RequestGroupMapper extends BaseMapper<RequestGroup> {

    /**
     * 根据群id和请求用户id查询未处理的请求
     *
     * @param groupId 群id
     * @param requestUserId 请求用户id
     * @return 请求记录
     */
    @Select("select * from t_request_group where group_id = #{groupId} " +
            "and request_user_id = #{requestUserId} and status = 0")
    RequestGroup findPendingRequest(@Param("groupId") Long groupId, 
                                    @Param("requestUserId") Long requestUserId);

    /**
     * 查询我发送的群组请求列表
     *
     * @param requestUserId 请求用户id
     * @return 请求记录列表
     */
    @Select("select * from t_request_group where request_user_id = #{requestUserId} " +
            "order by created_time desc")
    List<RequestGroup> findSentRequests(@Param("requestUserId") Long requestUserId);

    /**
     * 查询指定群组的所有请求列表（用于群主/管理员查看）
     *
     * @param groupId 群组id
     * @return 请求记录列表
     */
    @Select("select * from t_request_group where group_id = #{groupId} " +
            "order by created_time desc")
    List<RequestGroup> findGroupRequests(@Param("groupId") Long groupId);
    /**
     * 根据群组id列表查询请求（未处理 或 一个月内已处理）
     *
     * @param groupIds 群组id列表
     * @param oneMonthAgo 一个月前的时间
     * @return 请求记录列表
     */
    List<RequestGroup> findRequestsByGroupIds(@Param("groupIds") List<Long> groupIds, 
                                              @Param("oneMonthAgo") Date oneMonthAgo);
}




