package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.dao.entity.RequestFriendDO;
import com.jianhui.project.harbor.platform.dao.entity.User;
import com.jianhui.project.harbor.platform.dao.mapper.RequestFriendMapper;
import com.jianhui.project.harbor.platform.dto.request.AddFriendReqDTO;
import com.jianhui.project.harbor.platform.dto.request.DealFriendReqDTO;
import com.jianhui.project.harbor.platform.enums.AddType;
import com.jianhui.project.harbor.platform.enums.RequestStatus;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.service.FriendService;
import com.jianhui.project.harbor.platform.service.RequestFriendService;
import com.jianhui.project.harbor.platform.service.UserService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author wangj
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequestFriendServiceImpl extends ServiceImpl<RequestFriendMapper, RequestFriendDO>
        implements RequestFriendService {

    private final UserService userService;
    private final FriendService friendService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFriend(AddFriendReqDTO dto) {
        Long currentUserId = SessionContext.getSession().getUserId();
        Long targetUserId = dto.getReceiveUserId();

        // 不能添加自己
        if (currentUserId.equals(targetUserId)) {
            throw new GlobalException("不能添加自己为好友");
        }

        // 检查是否已经是好友
        if (friendService.isFriend(currentUserId, targetUserId)) {
            throw new GlobalException("对方已经是您的好友");
        }

        // 检查是否已有待处理的请求
        LambdaQueryWrapper<RequestFriendDO> wrapper = Wrappers.lambdaQuery(RequestFriendDO.class)
                .eq(RequestFriendDO::getRequestUserId, currentUserId)
                .eq(RequestFriendDO::getReceiveUserId, targetUserId)
                .eq(RequestFriendDO::getStatus, RequestStatus.PENDING.code());
        if (exists(wrapper)) {
            throw new GlobalException("已有待处理的好友请求");
        }

        // 获取目标用户的addType
        User targetUser = userService.getById(targetUserId);
        if (targetUser == null) {
            throw new GlobalException("用户不存在");
        }

        AddType addType = AddType.fromCode(targetUser.getAddType());

        switch (addType) {
            case DIRECT:
                // 直接添加好友
                friendService.addFriend(targetUserId);
                log.info("直接添加好友，用户id:{}, 好友id:{}", currentUserId, targetUserId);
                break;

            case APPROVAL:
                // 创建好友请求
                RequestFriendDO request = new RequestFriendDO();
                request.setRequestUserId(currentUserId);
                request.setReceiveUserId(targetUserId);
                request.setRequestNote(dto.getRequestNote());
                request.setStatus(RequestStatus.PENDING.code());
                request.setCreatedTime(new Date());
                save(request);
                log.info("发送好友请求，用户id:{}, 目标id:{}", currentUserId, targetUserId);
                break;

            case FORBIDDEN:
                throw new GlobalException("该用户禁止添加好友");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dealFriendRequest(DealFriendReqDTO dto) {
        Long currentUserId = SessionContext.getSession().getUserId();

        // 验证当前用户是接收方
        if (!currentUserId.equals(dto.getReceiveUserId())) {
            throw new GlobalException("无权处理此请求");
        }

        // 获取请求
        RequestFriendDO request = getById(dto.getId());
        if (request == null) {
            throw new GlobalException("请求不存在");
        }

        // 验证请求状态
        if (!RequestStatus.PENDING.code().equals(request.getStatus())) {
            throw new GlobalException("该请求已处理");
        }

        // 更新请求状态
        request.setStatus(dto.getStatus());
        request.setComment(dto.getComment());
        request.setDealTime(new Date());
        request.setUpdateTime(new Date());
        updateById(request);

        // 如果同意，则建立好友关系
        if (RequestStatus.ACCEPTED.code().equals(dto.getStatus())) {
            friendService.addFriend(request.getReceiveUserId());
            log.info("同意好友请求，请求id:{}", dto.getId());
        } else {
            log.info("拒绝好友请求，请求id:{}", dto.getId());
        }
    }

    @Override
    public List<RequestFriendDO> findSentRequests() {
        Long currentUserId = SessionContext.getSession().getUserId();
        LambdaQueryWrapper<RequestFriendDO> wrapper = Wrappers.lambdaQuery(RequestFriendDO.class)
                .eq(RequestFriendDO::getRequestUserId, currentUserId)
                .orderByDesc(RequestFriendDO::getCreatedTime);
        return list(wrapper);
    }

    @Override
    public List<RequestFriendDO> findReceivedRequests() {
        Long currentUserId = SessionContext.getSession().getUserId();
        LambdaQueryWrapper<RequestFriendDO> wrapper = Wrappers.lambdaQuery(RequestFriendDO.class)
                .eq(RequestFriendDO::getReceiveUserId, currentUserId)
                .orderByDesc(RequestFriendDO::getCreatedTime);
        return list(wrapper);
    }
}
