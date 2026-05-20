package com.jianhui.project.harbor.platform.service;

import com.jianhui.project.harbor.common.model.GroupMessageCreatedEvent;
import com.jianhui.project.harbor.platform.constant.Constant;
import com.jianhui.project.harbor.platform.dao.entity.GroupMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupMessageEventPreparer {

    private final GroupService groupService;
    private final GroupMemberService groupMemberService;

    public GroupMessageCreatedEvent prepare(GroupMessageCreatedEvent event) {
        if (event == null || event.getId() == null || event.getGroupId() == null || event.getSendId() == null) {
            log.warn("群聊消息事件缺少必要字段，event:{}", event);
            return null;
        }
        try {
            groupService.getAndCheckById(event.getGroupId());
        } catch (Exception e) {
            log.warn("群聊消息校验失败，群组不可用，msgId:{}, groupId:{}",
                    event.getId(), event.getGroupId(), e);
            return null;
        }

        GroupMember groupMember = groupMemberService.findByGroupAndUserId(event.getGroupId(), event.getSendId());
        if (groupMember == null || Boolean.TRUE.equals(groupMember.getQuit())) {
            log.warn("群聊消息校验失败，发送者已不在群中，msgId:{}, groupId:{}, sendId:{}",
                    event.getId(), event.getGroupId(), event.getSendId());
            return null;
        }

        List<Long> deliveryRecvIds = event.getDeliveryRecvIds();
        if (deliveryRecvIds == null) {
            deliveryRecvIds = groupMemberService.findUserIdsByGroupId(event.getGroupId()).stream()
                    .filter(userId -> !event.getSendId().equals(userId))
                    .toList();
            event.setDeliveryRecvIds(deliveryRecvIds);
        }

        if (Boolean.TRUE.equals(event.getReceipt()) && deliveryRecvIds.size() + 1 > Constant.MAX_LARGE_GROUP_MEMBER) {
            log.warn("群聊消息校验失败，超大群不支持回执消息，msgId:{}, groupId:{}, memberCount:{}",
                    event.getId(), event.getGroupId(), deliveryRecvIds.size() + 1);
            return null;
        }

        if (event.getSendNickname() == null) {
            event.setSendNickname(groupMember.getShowNickname());
        }
        return event;
    }
}
