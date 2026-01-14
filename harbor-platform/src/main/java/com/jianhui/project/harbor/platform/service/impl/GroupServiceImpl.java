package com.jianhui.project.harbor.platform.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.client.IMClient;
import com.jianhui.project.harbor.common.model.IMGroupMessage;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.common.util.CommaTextUtils;
import com.jianhui.project.harbor.platform.constant.Constant;
import com.jianhui.project.harbor.platform.constant.RedisKey;
import com.jianhui.project.harbor.platform.entity.*;
import com.jianhui.project.harbor.platform.enums.GroupRole;
import com.jianhui.project.harbor.platform.enums.MessageStatus;
import com.jianhui.project.harbor.platform.enums.MessageType;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.mapper.GroupMapper;
import com.jianhui.project.harbor.platform.mapper.GroupMessageMapper;
import com.jianhui.project.harbor.platform.pojo.vo.GroupInviteVO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupMemberVO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupMessageVO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupVO;
import com.jianhui.project.harbor.platform.service.FriendService;
import com.jianhui.project.harbor.platform.service.GroupMemberService;
import com.jianhui.project.harbor.platform.service.GroupService;
import com.jianhui.project.harbor.platform.service.UserService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@CacheConfig(cacheNames = RedisKey.IM_CACHE_GROUP)
@Slf4j
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    private final UserService userService;
    private final GroupMemberService groupMemberService;
    private final IMClient imClient;
    private final FriendService friendService;
    private final GroupMessageMapper groupMessageMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public GroupVO createGroup(GroupVO vo) {
        // 查用户
        UserSession session = SessionContext.getSession();
        User user = userService.getById(session.getUserId());
        // 保存群组数据
        Group group = BeanUtils.copyProperties(vo, Group.class);
        group.setOwnerId(user.getId());
        this.save(group);
        // 把群主加入群
        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(session.getUserId());
        member.setHeadImage(user.getHeadImageThumb());
        member.setUserNickname(user.getNickname());
        member.setRemarkNickname(vo.getRemarkNickname());
        member.setRemarkGroupName(vo.getRemarkGroupName());
        member.setRole(GroupRole.OWNER.code());
        groupMemberService.save(member);
        GroupVO groupVo = findById(group.getId());
        // 推送同步消息给自己的其他终端
        sendAddGroupMessage(groupVo, Lists.newArrayList(), true);
        // 返回
        log.info("创建群聊，群聊id:{},群聊名称:{}", group.getId(), group.getName());
        return groupVo;
    }

    @Override
    public void invite(GroupInviteVO vo) {
        UserSession session = SessionContext.getSession();
        // 拿到group并检查
        Group group = getAndCheckById(vo.getGroupId());
        // 校验
        GroupMember groupMember = groupMemberService.findByGroupAndUserId(vo.getGroupId(), session.getUserId());
        if (groupMember == null || groupMember.getQuit()) {
            throw new GlobalException("您不在群聊中,邀请失败");
        }
        // 群聊人数校验
        List<GroupMember> members = groupMemberService.findByGroupId(vo.getGroupId());
        long size = members.stream().filter(m -> !m.getQuit()).count();
        if (vo.getFriendIds().size() + size > Constant.MAX_LARGE_GROUP_MEMBER) {
            throw new GlobalException("群聊人数不能大于" + Constant.MAX_LARGE_GROUP_MEMBER + "人");
        }
        // 找出好友信息
        List<Friend> friends = friendService.findByFriendIds(vo.getFriendIds());
        if (vo.getFriendIds().size() != friends.size()) {
            throw new GlobalException("部分用户不是您的好友，邀请失败");
        }
        // 将friend信息处理成member
        List<GroupMember> newMemberList = friends.stream().map(f -> {
            Optional<GroupMember> optional = members.stream()
                    .filter(member -> member.getUserId().equals(f.getFriendId())).findFirst();
            // 之前没加入过的new一个
            GroupMember member = optional.orElseGet(GroupMember::new);
            member.setGroupId(group.getId());
            member.setUserId(f.getFriendId());
            member.setQuit(false);
            member.setCreatedTime(new Date());
            member.setHeadImage(f.getFriendHeadImage());
            member.setUserNickname(f.getFriendNickname());
            member.setRole(GroupRole.MEMBER.code());
            return member;
        }).toList();
        // 存库
        if (!newMemberList.isEmpty()) {
            groupMemberService.saveOrUpdateBatch(newMemberList);
        }
        // 给每个被邀请的friend发送消息
        for (GroupMember member : newMemberList) {
            GroupVO groupVO = convertToVO(group, member);
            sendAddGroupMessage(groupVO, List.of(member.getUserId()), false);
        }
        // 给群成员推送进入群聊消息
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(vo.getGroupId());
        String memberNames = newMemberList.stream().map(GroupMember::getShowNickname).collect(Collectors.joining(","));
        String content = String.format("'%s'邀请'%s'加入了群聊", session.getNickname(), memberNames);
        sendTipMessage(vo.getGroupId(), userIds, content, true);
        log.info("邀请进入群聊，群聊id:{},群聊名称:{},被邀请用户id:{}", group.getId(), group.getName(), vo.getFriendIds());
    }

    /**
     * 给群成员发送群聊的提示
     * 
     * @param groupId   群id
     * @param recvIds   需要接收的id
     * @param content   内容
     * @param sendToAll 是否发送给全部
     */
    private void sendTipMessage(@NotNull(message = "群id不可为空") Long groupId, List<Long> recvIds, String content,
            boolean sendToAll) {
        UserSession session = SessionContext.getSession();
        // 入库
        GroupMessage groupMessage = new GroupMessage();
        groupMessage.setSendId(session.getUserId());
        groupMessage.setRecvIds(sendToAll ? "" : CommaTextUtils.asText(recvIds));
        groupMessage.setGroupId(groupId);
        groupMessage.setContent(content);
        groupMessage.setType(MessageType.TIP_TEXT.code());
        groupMessage.setStatus(MessageStatus.SENDED.code());
        groupMessage.setSendTime(new Date());
        groupMessage.setSendNickname(session.getNickname());
        groupMessageMapper.insert(groupMessage);
        // 组织VO
        GroupMessageVO groupMessageVO = BeanUtils.copyProperties(groupMessage, GroupMessageVO.class);
        // 组装IMMsg
        IMGroupMessage<GroupMessageVO> imGroupMessage = new IMGroupMessage<>();
        imGroupMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        if (CollUtil.isEmpty(recvIds)) {
            // 为空表示向全体发送
            List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
            imGroupMessage.setRecvIds(userIds);
        } else {
            imGroupMessage.setRecvIds(recvIds);
        }
        imGroupMessage.setData(groupMessageVO);
        imGroupMessage.setIsSendBack(false);
        imGroupMessage.setSendToSelf(false);
        imClient.sendGroupMessage(imGroupMessage);
    }

    /**
     * 将group和member合并成GroupVO用于通知前端
     */
    private GroupVO convertToVO(Group group, GroupMember member) {
        GroupVO vo = BeanUtils.copyProperties(group, GroupVO.class);
        vo.setRemarkGroupName(member.getRemarkGroupName());
        vo.setRemarkNickname(member.getRemarkNickname());
        vo.setShowNickname(member.getShowNickname());
        vo.setShowGroupName(StrUtil.blankToDefault(member.getRemarkGroupName(), group.getName()));
        vo.setQuit(member.getQuit());
        return vo;
    }

    @CacheEvict(key = "#vo.getId()")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public GroupVO modifyGroup(GroupVO vo) {
        UserSession session = SessionContext.getSession();
        // 拿到group并校验
        Group group = getAndCheckById(vo.getId());
        // 校验是否成员
        GroupMember groupMember = groupMemberService.findByGroupAndUserId(vo.getId(), session.getUserId());
        if (groupMember == null || groupMember.getQuit()) {
            throw new GlobalException("您不是群聊的成员");
        }
        // 群成员可以更改群内昵称和群备注
        groupMember.setRemarkNickname(vo.getRemarkNickname());
        groupMember.setRemarkGroupName(vo.getRemarkGroupName());
        // 群主和管理员可以更改群聊基本信息
        if (groupMember.getRole().equals(GroupRole.ADMIN.code())
                || groupMember.getRole().equals(GroupRole.OWNER.code())) {
            group = BeanUtils.copyProperties(vo, Group.class);
            updateById(group);
        }
        log.info("修改群聊:userId:{},群聊id:{},群聊名称:{}", groupMember.getUserId(), group.getId(), group.getName());
        return convertToVO(group, groupMember);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(key = "#groupId")
    @Override
    public void deleteGroup(Long groupId) {
        UserSession session = SessionContext.getSession();
        Group group = this.getById(groupId);
        if (!group.getOwnerId().equals(session.getUserId())) {
            throw new GlobalException("只有群主才有权限解除群聊");
        }
        // 群聊Id
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
        // 逻辑解散
        group.setDissolve(true);
        updateById(group);
        // 移除群成员
        groupMemberService.removeByGroupId(groupId);
        // 清理已读缓存
        String key = StrUtil.join(":", RedisKey.IM_GROUP_READED_POSITION, groupId);
        redisTemplate.delete(key);
        // 推送解散群聊提示
        String content = String.format("'%s'解散了群聊", session.getNickname());
        sendTipMessage(groupId, userIds, content, true);
        sendDelGroupMessage(groupId, userIds, false);
        log.info("删除群聊，群主id:{},群聊id:{},群聊名称:{}", group.getOwnerId(), group.getId(), group.getName());
    }

    @Override
    public GroupVO findById(Long groupId) {
        UserSession session = SessionContext.getSession();
        Group group = getById(groupId);
        if (group == null) {
            throw new GlobalException("群组不存在");
        }
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, session.getUserId());
        if (member == null) {
            throw new GlobalException("您未加入群聊");
        }
        return convertToVO(group, member);
    }

    @Override
    public List<GroupVO> findGroups() {
        UserSession session = SessionContext.getSession();
        // 查询自己加的所有群
        List<GroupMember> groupMemberList = groupMemberService.findByUserId(session.getUserId());
        // 一个月内退的群可能存在退群前的离线消息,一并返回作为前端缓存
        groupMemberList.addAll(groupMemberService.findQuitInMonth(session.getUserId()));
        if (groupMemberList.isEmpty()) {
            return new LinkedList<>();
        }
        // 拉取群列表
        List<Long> groupIds = groupMemberList.stream().map(GroupMember::getGroupId).toList();
        LambdaQueryWrapper<Group> in = Wrappers.lambdaQuery(Group.class)
                .in(Group::getId, groupIds);
        List<Group> groups = list(in);
        // 转为VO
        return groups.stream().map(group -> {
            GroupMember groupMember = groupMemberList
                    .stream()
                    .filter(m -> m.getGroupId().equals(group.getId()))
                    .findFirst()
                    .get();
            return convertToVO(group, groupMember);
        }).toList();
    }

    @Cacheable(key = "#groupId")
    @Override
    public Group getAndCheckById(Long groupId) {
        Group group = super.getById(groupId);
        if (Objects.isNull(group)) {
            throw new GlobalException("群组不存在");
        }
        if (group.getDissolve()) {
            throw new GlobalException("群组'" + group.getName() + "'已解散");
        }
        if (group.getIsBanned()) {
            throw new GlobalException("群组'" + group.getName() + "'已被封禁,原因:" + group.getReason());
        }
        return group;
    }

    @Override
    public List<GroupMemberVO> findGroupMembers(Long groupId) {
        Group group = getAndCheckById(groupId);
        List<GroupMember> members = groupMemberService.findByGroupId(groupId);
        List<Long> userIds = members.stream().map(GroupMember::getUserId).toList();
        List<Long> onlineUserIds = imClient.getOnlineUser(userIds);
        // 返回VO，在线的排前面
        return members.stream().map(m -> {
            GroupMemberVO vo = BeanUtils.copyProperties(m, GroupMemberVO.class);
            vo.setShowNickname(m.getShowNickname());
            vo.setShowGroupName(StrUtil.blankToDefault(m.getRemarkGroupName(), group.getName()));
            vo.setOnline(onlineUserIds.contains(m.getUserId()));
            return vo;
        }).sorted((m1, m2) -> m2.getOnline().compareTo(m1.getOnline())).toList();
    }

    @Override
    public GroupVO searchById(Long groupId) {
        Group group = getById(groupId);
        return BeanUtils.copyProperties(group, GroupVO.class);
    }

    @Override
    public void quitGroup(Long groupId) {
        Long userId = SessionContext.getSession().getUserId();
        Group group = this.getById(groupId);
        if (group.getOwnerId().equals(userId)) {
            throw new GlobalException("您是群主，不可退出群聊");
        }
        // 删除群聊成员
        groupMemberService.removeByGroupAndUserId(groupId, userId);
        // 清理已读缓存
        String key = StrUtil.join(":", RedisKey.IM_GROUP_READED_POSITION, groupId);
        redisTemplate.opsForHash().delete(key, userId.toString());
        // 推送退出群聊提示
        sendTipMessage(groupId, List.of(userId), "您已退出群聊", false);
        // 推送同步消息
        sendDelGroupMessage(groupId, Lists.newArrayList(), true);
        log.info("退出群聊，群聊id:{},群聊名称:{},用户id:{}", group.getId(), group.getName(), userId);
    }

    @Override
    public void kickFromGroup(Long groupId, Long userId) {
        UserSession session = SessionContext.getSession();
        Group group = this.getAndCheckById(groupId);

        // 获取操作者的群成员信息
        GroupMember operatorMember = groupMemberService.findByGroupAndUserId(groupId, session.getUserId());
        if (operatorMember == null || operatorMember.getQuit()) {
            throw new GlobalException("您不在群聊中");
        }

        // 校验操作者权限：只有群主和管理员才能踢人
        boolean isOwner = group.getOwnerId().equals(session.getUserId());
        boolean isAdmin = GroupRole.ADMIN.code().equals(operatorMember.getRole());
        if (!isOwner && !isAdmin) {
            throw new GlobalException("只有群主和管理员才有权限踢人");
        }

        // 不能移除自己
        if (userId.equals(session.getUserId())) {
            throw new GlobalException("不能移除自己");
        }

        // 获取被踢者的群成员信息
        GroupMember targetMember = groupMemberService.findByGroupAndUserId(groupId, userId);
        if (targetMember == null || targetMember.getQuit()) {
            throw new GlobalException("该用户不在群聊中");
        }

        // 校验被踢者权限：不能踢群主；管理员不能踢管理员，群主可以踢管理员
        boolean targetIsOwner = group.getOwnerId().equals(userId);
        boolean targetIsAdmin = GroupRole.ADMIN.code().equals(targetMember.getRole());
        if (targetIsOwner) {
            throw new GlobalException("不能移除群主");
        }
        if (targetIsAdmin && !isOwner) {
            throw new GlobalException("只有群主才能移除管理员");
        }

        // 删除群聊成员
        groupMemberService.removeByGroupAndUserId(groupId, userId);
        // 清理已读缓存
        String key = String.join(":", RedisKey.IM_GROUP_READED_POSITION, groupId.toString());
        redisTemplate.opsForHash().delete(key, userId.toString());
        // 推送踢出群聊提示
        this.sendTipMessage(groupId, List.of(userId), "您已被移出群聊", false);
        // 推送同步消息
        this.sendDelGroupMessage(groupId, List.of(userId), false);
        log.info("踢出群聊，群聊id:{},群聊名称:{},用户id:{}", group.getId(), group.getName(), userId);
    }

    /**
     * 发送添加群组信息
     * 
     * @param groupVo
     * @param recvIds
     * @param sendToSelf
     */
    private void sendAddGroupMessage(GroupVO groupVo, List<Long> recvIds, Boolean sendToSelf) {
        UserSession session = SessionContext.getSession();
        GroupMessageVO msgInfo = new GroupMessageVO();
        msgInfo.setSendId(session.getUserId());
        msgInfo.setGroupId(groupVo.getId());
        msgInfo.setSendTime(new Date());
        msgInfo.setType(MessageType.GROUP_NEW.code());
        msgInfo.setContent(JSON.toJSONString(groupVo));
        IMGroupMessage<GroupMessageVO> imGroupMsg = new IMGroupMessage<>();
        imGroupMsg.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        imGroupMsg.setRecvIds(recvIds);
        imGroupMsg.setSendToSelf(sendToSelf);
        imGroupMsg.setData(msgInfo);
        imGroupMsg.setIsSendBack(false);
        imClient.sendGroupMessage(imGroupMsg);
    }

    /**
     * 发送删除群组信息
     * 
     * @param groupId
     * @param recvIds
     * @param sendToSelf
     */
    private void sendDelGroupMessage(Long groupId, List<Long> recvIds, Boolean sendToSelf) {
        UserSession session = SessionContext.getSession();
        GroupMessageVO msgInfo = new GroupMessageVO();
        msgInfo.setSendId(session.getUserId());
        msgInfo.setGroupId(groupId);
        msgInfo.setSendTime(new Date());
        msgInfo.setType(MessageType.GROUP_DEL.code());
        IMGroupMessage<GroupMessageVO> imGroupMsg = new IMGroupMessage<>();
        imGroupMsg.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        imGroupMsg.setRecvIds(recvIds);
        imGroupMsg.setSendToSelf(sendToSelf);
        imGroupMsg.setData(msgInfo);
        imGroupMsg.setIsSendBack(false);
        imClient.sendGroupMessage(imGroupMsg);
    }

    @Override
    public void setAdmin(Long groupId, Long userId, Boolean isAdmin) {
        UserSession session = SessionContext.getSession();
        Group group = this.getAndCheckById(groupId);

        // 只有群主可以设置管理员
        if (!group.getOwnerId().equals(session.getUserId())) {
            throw new GlobalException("只有群主才能设置管理员");
        }

        // 不能对自己操作
        if (userId.equals(session.getUserId())) {
            throw new GlobalException("不能对自己进行此操作");
        }

        // 获取目标成员信息
        GroupMember targetMember = groupMemberService.findByGroupAndUserId(groupId, userId);
        if (targetMember == null || targetMember.getQuit()) {
            throw new GlobalException("该用户不在群聊中");
        }

        // 不能对群主操作
        if (group.getOwnerId().equals(userId)) {
            throw new GlobalException("不能对群主进行此操作");
        }

        // 设置角色
        Integer newRole = isAdmin ? GroupRole.ADMIN.code() : GroupRole.MEMBER.code();
        targetMember.setRole(newRole);
        groupMemberService.updateById(targetMember);

        // 推送提示消息
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
        String action = isAdmin ? "设置" : "取消";
        String content = String.format("'%s'已被%s为管理员", targetMember.getShowNickname(), action);
        sendTipMessage(groupId, userIds, content, true);

        log.info("{}管理员，群聊id:{},用户id:{}", action, groupId, userId);
    }
}
