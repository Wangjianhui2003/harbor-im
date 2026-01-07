package com.jianhui.project.harbor.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jianhui.project.harbor.platform.entity.RequestFriend;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RequestFriendMapper extends BaseMapper<RequestFriend> {

    /**
     * 根据请求用户id和接收用户id查询未处理的请求
     *
     * @param requestUserId 请求用户id
     * @param receiveUserId 接收用户id
     * @return 请求记录
     */
    @Select("select * from t_request_friend where request_user_id = #{requestUserId} " +
            "and receive_user_id = #{receiveUserId} and status = 0")
    RequestFriend findPendingRequest(@Param("requestUserId") Long requestUserId, 
                                     @Param("receiveUserId") Long receiveUserId);

    /**
     * 查询我发送的好友请求列表
     *
     * @param requestUserId 请求用户id
     * @return 请求记录列表
     */
    @Select("select * from t_request_friend where request_user_id = #{requestUserId} " +
            "order by created_time desc")
    List<RequestFriend> findSentRequests(@Param("requestUserId") Long requestUserId);

    /**
     * 查询我接收的好友请求列表
     *
     * @param receiveUserId 接收用户id
     * @return 请求记录列表
     */
    @Select("select * from t_request_friend where receive_user_id = #{receiveUserId} " +
            "order by created_time desc")
    List<RequestFriend> findReceivedRequests(@Param("receiveUserId") Long receiveUserId);
}




