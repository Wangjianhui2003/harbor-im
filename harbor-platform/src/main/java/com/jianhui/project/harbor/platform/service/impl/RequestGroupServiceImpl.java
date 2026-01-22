package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.constant.Constant;
import com.jianhui.project.harbor.platform.dao.entity.Group;
import com.jianhui.project.harbor.platform.dao.entity.GroupMember;
import com.jianhui.project.harbor.platform.dao.entity.RequestGroupDO;
import com.jianhui.project.harbor.platform.dao.entity.User;
import com.jianhui.project.harbor.platform.dao.mapper.RequestGroupMapper;
import com.jianhui.project.harbor.platform.dto.request.AddGroupReqDTO;
import com.jianhui.project.harbor.platform.dto.request.DealGroupReqDTO;
import com.jianhui.project.harbor.platform.enums.GroupRole;
import com.jianhui.project.harbor.platform.enums.JoinType;
import com.jianhui.project.harbor.platform.enums.RequestStatus;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.service.GroupMemberService;
import com.jianhui.project.harbor.platform.service.GroupService;
import com.jianhui.project.harbor.platform.service.RequestGroupService;
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
public class RequestGroupServiceImpl extends ServiceImpl<RequestGroupMapper, RequestGroupDO>
        implements RequestGroupService {

    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGroup(AddGroupReqDTO dto) {
        Long currentUserId = SessionContext.getSession().getUserId();
        Long groupId = dto.getGroupId();

        // 检查群组是否存在
        Group group = groupService.getAndCheckById(groupId);

        // 检查是否已在群中
        GroupMember existingMember = groupMemberService.findByGroupAndUserId(groupId, currentUserId);
        if (existingMember != null && !existingMember.getQuit()) {
            throw new GlobalException("您已在群聊中");
        }

        // 检查是否已有待处理的请求
        LambdaQueryWrapper<RequestGroupDO> wrapper = Wrappers.lambdaQuery(RequestGroupDO.class)
                .eq(RequestGroupDO::getGroupId, groupId)
                .eq(RequestGroupDO::getRequestUserId, currentUserId)
                .eq(RequestGroupDO::getStatus, RequestStatus.PENDING.code());
        if (exists(wrapper)) {
            throw new GlobalException("已有待处理的加群请求");
        }

        JoinType joinType = JoinType.fromCode(group.getJoinType());

        switch (joinType) {
            case DIRECT:
                // 直接加入群聊
                addMemberToGroup(groupId, currentUserId);
                log.info("直接加入群聊，用户id:{}, 群组id:{}", currentUserId, groupId);
                break;

            case APPROVAL:
                // 创建入群请求
                RequestGroupDO request = new RequestGroupDO();
                request.setGroupId(groupId);
                request.setRequestUserId(currentUserId);
                request.setRequestNote(dto.getRequestNote());
                request.setStatus(RequestStatus.PENDING.code());
                request.setCreatedTime(new Date());
                save(request);
                log.info("发送入群请求，用户id:{}, 群组id:{}", currentUserId, groupId);
                break;

            case FORBIDDEN:
                throw new GlobalException("该群聊禁止加入");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dealGroupRequest(DealGroupReqDTO dto) {
        Long currentUserId = SessionContext.getSession().getUserId();
        Long groupId = dto.getGroupId();

        // 验证当前用户是群主或管理员
        GroupMember currentMember = groupMemberService.findByGroupAndUserId(groupId, currentUserId);
        if (currentMember == null || currentMember.getQuit()) {
            throw new GlobalException("您不在群聊中");
        }
        if (!GroupRole.OWNER.code().equals(currentMember.getRole())
                && !GroupRole.ADMIN.code().equals(currentMember.getRole())) {
            throw new GlobalException("只有群主或管理员可以处理加群请求");
        }

        // 使用乐观锁更新请求状态
        LambdaUpdateWrapper<RequestGroupDO> updateWrapper = Wrappers.lambdaUpdate(RequestGroupDO.class)
                .eq(RequestGroupDO::getId, dto.getId())
                .eq(RequestGroupDO::getStatus, RequestStatus.PENDING.code())
                .set(RequestGroupDO::getStatus, dto.getStatus())
                .set(RequestGroupDO::getDealUserId, currentUserId)
                .set(RequestGroupDO::getComment, dto.getComment())
                .set(RequestGroupDO::getDealTime, new Date())
                .set(RequestGroupDO::getUpdateTime, new Date());

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new GlobalException("该请求已被处理或不存在");
        }

        // 如果同意，则添加成员到群聊
        if (RequestStatus.ACCEPTED.code().equals(dto.getStatus())) {
            // 检查群人数上限
            List<GroupMember> members = groupMemberService.findByGroupId(groupId);
            long activeCount = members.stream().filter(m -> !m.getQuit()).count();
            if (activeCount >= Constant.MAX_NORMAL_GROUP_MEMBER) {
                // 回滚状态为待处理
                LambdaUpdateWrapper<RequestGroupDO> rollbackWrapper = Wrappers.lambdaUpdate(RequestGroupDO.class)
                        .eq(RequestGroupDO::getId, dto.getId())
                        .set(RequestGroupDO::getStatus, RequestStatus.PENDING.code())
                        .set(RequestGroupDO::getDealUserId, null)
                        .set(RequestGroupDO::getDealTime, null);
                update(rollbackWrapper);
                throw new GlobalException("群聊人数已达上限");
            }

            addMemberToGroup(groupId, dto.getRequestUserId());
            log.info("同意入群请求，请求id:{}, 处理人:{}", dto.getId(), currentUserId);
        } else {
            log.info("拒绝入群请求，请求id:{}, 处理人:{}", dto.getId(), currentUserId);
        }
    }

    /**
     * 添加成员到群聊
     */
    private void addMemberToGroup(Long groupId, Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new GlobalException("用户不存在");
        }

        GroupMember existingMember = groupMemberService.findByGroupAndUserId(groupId, userId);
        GroupMember member = existingMember != null ? existingMember : new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setUserNickname(user.getNickname());
        member.setHeadImage(user.getHeadImage());
        member.setRole(GroupRole.MEMBER.code());
        member.setQuit(false);
        member.setCreatedTime(new Date());

        groupMemberService.saveOrUpdate(member);
    }

    @Override
    public List<RequestGroupDO> findSentRequests() {
        Long currentUserId = SessionContext.getSession().getUserId();
        LambdaQueryWrapper<RequestGroupDO> wrapper = Wrappers.lambdaQuery(RequestGroupDO.class)
                .eq(RequestGroupDO::getRequestUserId, currentUserId)
                .orderByDesc(RequestGroupDO::getCreatedTime);
        return list(wrapper);
    }

    @Override
    public List<RequestGroupDO> findByGroupId(Long groupId) {
        LambdaQueryWrapper<RequestGroupDO> wrapper = Wrappers.lambdaQuery(RequestGroupDO.class)
                .eq(RequestGroupDO::getGroupId, groupId)
                .orderByDesc(RequestGroupDO::getCreatedTime);
        return list(wrapper);
    }

    @Override
    public List<RequestGroupDO> findByGroupIds(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<RequestGroupDO> wrapper = Wrappers.lambdaQuery(RequestGroupDO.class)
                .in(RequestGroupDO::getGroupId, groupIds)
                .orderByDesc(RequestGroupDO::getCreatedTime);
        return list(wrapper);
    }
}
