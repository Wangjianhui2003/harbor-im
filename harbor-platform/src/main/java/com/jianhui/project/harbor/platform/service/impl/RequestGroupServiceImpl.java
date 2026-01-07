package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.entity.Group;
import com.jianhui.project.harbor.platform.entity.GroupMember;
import com.jianhui.project.harbor.platform.entity.RequestGroup;
import com.jianhui.project.harbor.platform.entity.User;
import com.jianhui.project.harbor.platform.enums.GroupRole;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.mapper.RequestGroupMapper;
import com.jianhui.project.harbor.platform.service.GroupMemberService;
import com.jianhui.project.harbor.platform.service.GroupService;
import com.jianhui.project.harbor.platform.service.RequestGroupService;
import com.jianhui.project.harbor.platform.service.UserService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestGroupServiceImpl extends ServiceImpl<RequestGroupMapper, RequestGroup> implements RequestGroupService {

    private final RequestGroupMapper requestGroupMapper;
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGroupRequest(RequestGroup requestGroup) {
        UserSession session = SessionContext.getSession();
        Long currentUserId = session.getUserId();

        // 设置请求用户id为当前登录用户
        requestGroup.setRequestUserId(currentUserId);

        // 检查群是否存在
        Group group = groupService.getById(requestGroup.getGroupId());
        if (group == null) {
            throw new GlobalException("群组不存在");
        }

        // 检查群是否已解散或被封禁
        if (group.getDissolve() != null && group.getDissolve()) {
            throw new GlobalException("群组已解散");
        }
        if (group.getIsBanned() != null && group.getIsBanned()) {
            throw new GlobalException("群组已被封禁");
        }

        // 检查用户是否已经在群中
        GroupMember existingMember = groupMemberService.findByGroupAndUserId(
                requestGroup.getGroupId(), currentUserId);
        if (existingMember != null && (existingMember.getQuit() == null || !existingMember.getQuit())) {
            throw new GlobalException("您已经在该群组中");
        }

        // 检查是否已有未处理的请求
        RequestGroup pendingRequest = requestGroupMapper.findPendingRequest(
                requestGroup.getGroupId(), currentUserId);
        if (pendingRequest != null) {
            throw new GlobalException("已存在待处理的群组请求");
        }

        requestGroup.setCreatedTime(new Date());
        requestGroup.setUpdateTime(new Date());

        // 如果群的 joinType 为 0（直接加入），则直接同意并添加群成员
        if (group.getJoinType() != null && group.getJoinType() == 0) {
            // 检查请求用户是否已经在群中
            GroupMember requestUserMember = groupMemberService.findByGroupAndUserId(
                    requestGroup.getGroupId(), currentUserId);
            if (requestUserMember != null && (requestUserMember.getQuit() == null || !requestUserMember.getQuit())) {
                throw new GlobalException("您已经在该群组中");
            }

            // 获取请求用户信息
            User requestUser = userService.getById(currentUserId);
            if (requestUser == null) {
                throw new GlobalException("请求用户不存在");
            }
            // 创建或更新群成员
            GroupMember member = requestUserMember;
            if (member == null) {
                member = new GroupMember();
                member.setCreatedTime(new Date());
            }
            member.setGroupId(requestGroup.getGroupId());
            member.setUserId(currentUserId);
            member.setUserNickname(requestUser.getNickname());
            member.setHeadImage(requestUser.getHeadImageThumb());
            member.setRole(GroupRole.MEMBER.code());
            member.setQuit(false);
            member.setUpdateTime(new Date());
            groupMemberService.saveOrUpdate(member);

            // 保存请求记录，状态为已同意
            requestGroup.setStatus(1);
            requestGroup.setDealTime(new Date());
            requestGroup.setComment("自动同意");
            // 如果是直接加入，dealUserId 可以设置为群主或空
            if (group.getOwnerId() != null) {
                requestGroup.setDealUserId(group.getOwnerId());
            }
            save(requestGroup);
            log.info("直接加入群组（joinType=0），群组id:{},请求用户id:{}", requestGroup.getGroupId(), currentUserId);
        } else {
            // 设置状态为未处理，等待管理员同意
            requestGroup.setStatus(0);
            save(requestGroup);
            log.info("发送添加群组请求，群组id:{},请求用户id:{}", requestGroup.getGroupId(), currentUserId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dealGroupRequest(RequestGroup requestGroup) {
        UserSession session = SessionContext.getSession();
        Long currentUserId = session.getUserId();

        // 校验 id 不能为空
        if (requestGroup.getId() == null) {
            throw new GlobalException("请求id不能为空");
        }

        // 根据 id 查询请求记录
        RequestGroup existingRequest = getById(requestGroup.getId());
        if (existingRequest == null) {
            throw new GlobalException("未找到该群组请求");
        }

        // 校验请求状态必须是未处理
        if (existingRequest.getStatus() == null || existingRequest.getStatus() != 0) {
            throw new GlobalException("该请求已处理或状态异常");
        }

        // 检查群是否存在
        Group group = groupService.getById(existingRequest.getGroupId());
        if (group == null) {
            throw new GlobalException("群组不存在");
        }

        // 检查群是否已解散或被封禁
        if (group.getDissolve() != null && group.getDissolve()) {
            throw new GlobalException("群组已解散");
        }
        if (group.getIsBanned() != null && group.getIsBanned()) {
            throw new GlobalException("群组已被封禁");
        }

        // 检查处理人是否有权限（群主或管理员）
        GroupMember dealMember = groupMemberService.findByGroupAndUserId(
                existingRequest.getGroupId(), currentUserId);
        if (dealMember == null || dealMember.getQuit() != null && dealMember.getQuit()) {
            throw new GlobalException("您不是该群组的成员，无法处理请求");
        }

        // 检查是否需要管理员同意（joinType = 1）
        if (group.getJoinType() != null && group.getJoinType() == 1) {
            // 需要管理员同意，检查处理人是否是群主或管理员
            if (dealMember.getRole() == null || 
                (!dealMember.getRole().equals(GroupRole.OWNER.code()) && 
                 !dealMember.getRole().equals(GroupRole.ADMIN.code()))) {
                throw new GlobalException("只有群主或管理员可以处理加入请求");
            }
        } else {
            // 不需要管理员同意，但处理人必须是群主
            if (dealMember.getRole() == null || !dealMember.getRole().equals(GroupRole.OWNER.code())) {
                throw new GlobalException("只有群主可以处理加入请求");
            }
        }

        // 校验状态值（1:同意 2:拒绝）
        if (requestGroup.getStatus() == null || 
            (requestGroup.getStatus() != 1 && requestGroup.getStatus() != 2)) {
            throw new GlobalException("状态值无效，1:同意 2:拒绝");
        }

        // 更新请求记录
        existingRequest.setStatus(requestGroup.getStatus());
        existingRequest.setDealUserId(currentUserId);
        existingRequest.setComment(requestGroup.getComment());
        existingRequest.setDealTime(new Date());
        existingRequest.setUpdateTime(new Date());
        updateById(existingRequest);

        // 如果同意，则添加群成员
        if (requestGroup.getStatus() == 1) {
            // 检查请求用户是否已经在群中
            GroupMember requestUserMember = groupMemberService.findByGroupAndUserId(
                    existingRequest.getGroupId(), existingRequest.getRequestUserId());
            if (requestUserMember != null && (requestUserMember.getQuit() == null || !requestUserMember.getQuit())) {
                throw new GlobalException("用户已在群组中");
            }

            // 获取请求用户信息
            User requestUser = userService.getById(existingRequest.getRequestUserId());
            if (requestUser == null) {
                throw new GlobalException("请求用户不存在");
            }

            // 创建或更新群成员
            GroupMember member = requestUserMember;
            if (member == null) {
                member = new GroupMember();
                member.setCreatedTime(new Date());
            }
            member.setGroupId(existingRequest.getGroupId());
            member.setUserId(existingRequest.getRequestUserId());
            member.setUserNickname(requestUser.getNickname());
            member.setHeadImage(requestUser.getHeadImageThumb());
            member.setRole(GroupRole.MEMBER.code());
            member.setQuit(false);
            member.setUpdateTime(new Date());
            groupMemberService.saveOrUpdate(member);

            log.info("同意添加群组请求，请求id:{},群组id:{},请求用户id:{},处理用户id:{}", 
                    existingRequest.getId(), existingRequest.getGroupId(), 
                    existingRequest.getRequestUserId(), currentUserId);
        } else {
            log.info("拒绝添加群组请求，请求id:{},群组id:{},请求用户id:{},处理用户id:{}", 
                    existingRequest.getId(), existingRequest.getGroupId(), 
                    existingRequest.getRequestUserId(), currentUserId);
        }
    }

    @Override
    public List<RequestGroup> findSentRequests() {
        UserSession session = SessionContext.getSession();
        Long currentUserId = session.getUserId();
        return requestGroupMapper.findSentRequests(currentUserId);
    }

    @Override
    public List<RequestGroup> findGroupRequests(Long groupId) {
        // 校验群组是否存在
        Group group = groupService.getById(groupId);
        if (group == null) {
            throw new GlobalException("群组不存在");
        }

        // 校验当前用户是否有权限查看（必须是群成员）
        UserSession session = SessionContext.getSession();
        Long currentUserId = session.getUserId();
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, currentUserId);
        if (member == null || (member.getQuit() != null && member.getQuit()) || !member.getRole().equals(GroupRole.OWNER.code()) || !member.getRole().equals(GroupRole.ADMIN.code())) {
            throw new GlobalException("用户不是群主或管理员，无法查看请求");
        }

        return requestGroupMapper.findGroupRequests(groupId);
    }

    @Override
    public List<RequestGroup> findRequestsByGroupIds(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }
        
        // 计算一个月前的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        Date oneMonthAgo = calendar.getTime();
        
        
        return requestGroupMapper.findRequestsByGroupIds(groupIds, oneMonthAgo);
    }
}
