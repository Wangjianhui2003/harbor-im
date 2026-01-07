package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.entity.RequestFriend;
import com.jianhui.project.harbor.platform.entity.User;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.mapper.RequestFriendMapper;
import com.jianhui.project.harbor.platform.service.FriendService;
import com.jianhui.project.harbor.platform.service.RequestFriendService;
import com.jianhui.project.harbor.platform.service.UserService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestFriendServiceImpl extends ServiceImpl<RequestFriendMapper, RequestFriend> implements RequestFriendService {

    private final RequestFriendMapper requestFriendMapper;
    private final FriendService friendService;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFriendRequest(RequestFriend requestFriend) {
        UserSession session = SessionContext.getSession();
        Long currentUserId = session.getUserId();

        // 设置请求用户id为当前登录用户
        requestFriend.setRequestUserId(currentUserId);

        // 校验不能添加自己为好友
        if (requestFriend.getReceiveUserId().equals(currentUserId)) {
            throw new GlobalException("不能添加自己为好友");
        }

        // 检查是否已经是好友
        if (friendService.isFriend(currentUserId, requestFriend.getReceiveUserId())) {
            throw new GlobalException("你们已经是好友了");
        }

        // 检查是否已有未处理的请求
        RequestFriend pendingRequest = requestFriendMapper.findPendingRequest(
                currentUserId, requestFriend.getReceiveUserId());
        if (pendingRequest != null) {
            throw new GlobalException("已存在待处理的好友请求");
        }

        // 获取接收用户信息，检查 addType
        User receiveUser = userService.getById(requestFriend.getReceiveUserId());
        if (receiveUser == null) {
            throw new GlobalException("接收用户不存在");
        }

        requestFriend.setCreatedTime(new Date());
        requestFriend.setUpdateTime(new Date());

        // 如果接收用户的 addType 为 0（直接加好友），则直接同意
        if (receiveUser.getAddType() != null && receiveUser.getAddType() == 0) {
            // 保存请求记录，状态为已同意
            requestFriend.setStatus(1);
            requestFriend.setDealTime(new Date());
            requestFriend.setComment("自动同意");
            save(requestFriend);
            // 直接添加好友关系
            friendService.addFriend(requestFriend.getReceiveUserId());
            log.info("直接添加好友（addType=0），请求用户id:{},接收用户id:{}", currentUserId, requestFriend.getReceiveUserId());
        } else {
            // 设置状态为未处理，等待对方同意
            requestFriend.setStatus(0);
            save(requestFriend);
            log.info("发送添加好友请求，请求用户id:{},接收用户id:{}", currentUserId, requestFriend.getReceiveUserId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dealFriendRequest(RequestFriend requestFriend) {
        UserSession session = SessionContext.getSession();
        Long currentUserId = session.getUserId();

        // 校验 id 不能为空
        if (requestFriend.getId() == null) {
            throw new GlobalException("请求id不能为空");
        }

        // 根据 id 查询请求记录
        RequestFriend existingRequest = getById(requestFriend.getId());
        if (existingRequest == null) {
            throw new GlobalException("未找到该好友请求");
        }

        // 校验请求状态必须是未处理
        if (existingRequest.getStatus() == null || existingRequest.getStatus() != 0) {
            throw new GlobalException("该请求已处理或状态异常");
        }

        // 校验接收用户必须是当前登录用户
        if (!existingRequest.getReceiveUserId().equals(currentUserId)) {
            throw new GlobalException("只能处理发送给自己的好友请求");
        }

        // 校验状态值（1:同意 2:拒绝）
        if (requestFriend.getStatus() == null || 
            (requestFriend.getStatus() != 1 && requestFriend.getStatus() != 2)) {
            throw new GlobalException("状态值无效，1:同意 2:拒绝");
        }

        // 更新请求记录
        existingRequest.setStatus(requestFriend.getStatus());
        existingRequest.setComment(requestFriend.getComment());
        existingRequest.setDealTime(new Date());
        existingRequest.setUpdateTime(new Date());
        updateById(existingRequest);

        // 如果同意，则添加好友关系
        if (requestFriend.getStatus() == 1) {
            friendService.addFriend(existingRequest.getRequestUserId());
            log.info("同意添加好友请求，请求id:{},请求用户id:{},接收用户id:{}", 
                    existingRequest.getId(), existingRequest.getRequestUserId(), currentUserId);
        } else {
            log.info("拒绝添加好友请求，请求id:{},请求用户id:{},接收用户id:{}", 
                    existingRequest.getId(), existingRequest.getRequestUserId(), currentUserId);
        }
    }

    @Override
    public List<RequestFriend> findSentRequests() {
        UserSession session = SessionContext.getSession();
        Long currentUserId = session.getUserId();
        return requestFriendMapper.findSentRequests(currentUserId);
    }

    @Override
    public List<RequestFriend> findReceivedRequests() {
        UserSession session = SessionContext.getSession();
        Long currentUserId = session.getUserId();
        return requestFriendMapper.findReceivedRequests(currentUserId);
    }
}
