package com.jianhui.project.harbor.platform.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.client.IMClient;
import com.jianhui.project.harbor.common.constant.IMConstant;
import com.jianhui.project.harbor.common.enums.IMTerminalType;
import com.jianhui.project.harbor.common.model.IMGroupMessage;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.common.util.CommaTextUtils;
import com.jianhui.project.harbor.platform.constant.Constant;
import com.jianhui.project.harbor.platform.constant.RedisKey;
import com.jianhui.project.harbor.platform.dao.entity.Group;
import com.jianhui.project.harbor.platform.dao.entity.GroupMember;
import com.jianhui.project.harbor.platform.dao.entity.GroupMessage;
import com.jianhui.project.harbor.platform.dao.mapper.GroupMessageMapper;
import com.jianhui.project.harbor.platform.dto.request.GroupMessageDTO;
import com.jianhui.project.harbor.platform.dto.response.GroupMessageRespDTO;
import com.jianhui.project.harbor.platform.enums.MessageStatus;
import com.jianhui.project.harbor.platform.enums.MessageType;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.service.GroupMemberService;
import com.jianhui.project.harbor.platform.service.GroupMessageService;
import com.jianhui.project.harbor.platform.service.GroupService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageServiceImpl extends ServiceImpl<GroupMessageMapper, GroupMessage> implements GroupMessageService{

    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final IMClient imClient;
    private final StringRedisTemplate redisTemplate;
    private final GroupMessageMapper groupMessageMapper;

    @Override
    public GroupMessageRespDTO sendMessage(GroupMessageDTO dto) {
        UserSession session = SessionContext.getSession();
        Group group = groupService.getAndCheckById(dto.getGroupId());
        // 是否在群聊里面
        GroupMember groupMember = groupMemberService.findByGroupAndUserId(group.getId(), session.getUserId());
        if(groupMember == null || groupMember.getQuit()){
            throw new GlobalException("您已不在群聊里，无法发送消息");
        }
        // 群聊成员列表
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(group.getId());
        // 人数太多不能发回执消息
        if (dto.getReceipt() && userIds.size() > Constant.MAX_LARGE_GROUP_MEMBER) {
            // 大群的回执消息过于消耗资源，不允许发送
            throw new GlobalException(String.format("当前群聊大于%s人,不支持发送回执消息", Constant.MAX_LARGE_GROUP_MEMBER));
        }
        // 不用发给自己
        userIds = userIds.stream().filter(id -> !session.getUserId().equals(id)).toList();
        // 保存消息
        GroupMessage groupMessage = BeanUtils.copyProperties(dto, GroupMessage.class);
        groupMessage.setSendId(session.getUserId());
        groupMessage.setSendTime(new Date());
        groupMessage.setSendNickname(groupMember.getShowNickname());
        groupMessage.setAtUserIds(CommaTextUtils.asText(dto.getAtUserIds()));
        groupMessage.setContent(dto.getContent());
        save(groupMessage);
        // 群发
        GroupMessageRespDTO msgVO = BeanUtils.copyProperties(groupMessage, GroupMessageRespDTO.class);
        msgVO.setAtUserIds(dto.getAtUserIds());
        IMGroupMessage<GroupMessageRespDTO> imMsg = new IMGroupMessage<>();
        imMsg.setSender(new IMUserInfo(session.getUserId(),session.getTerminal()));
        imMsg.setRecvIds(userIds);
        imMsg.setData(msgVO);
        imClient.sendGroupMessage(imMsg);
        log.info("发送群聊消息，发送id:{},群聊id:{},内容:{}", session.getUserId(), dto.getGroupId(), dto.getContent());
        return msgVO;
    }

    @Transactional
    @Override
    public GroupMessageRespDTO recallMessage(Long id) {
        UserSession session = SessionContext.getSession();
        GroupMessage msg = getById(id);
        if (Objects.isNull(msg)) {
            throw new GlobalException("消息不存在");
        }
        if (!msg.getSendId().equals(session.getUserId())) {
            throw new GlobalException("这条消息不是由您发送,无法撤回");
        }
        if (System.currentTimeMillis() - msg.getSendTime().getTime() > IMConstant.ALLOW_RECALL_SECOND * 1000) {
            throw new GlobalException("消息已发送超过5分钟，无法撤回");
        }
        // 判断是否在群里
        GroupMember member = groupMemberService.findByGroupAndUserId(msg.getGroupId(), session.getUserId());
        if (member == null || Boolean.TRUE.equals(member.getQuit())) {
            throw new GlobalException("您已不在群聊里面，无法撤回消息");
        }
        //修改
        msg.setStatus(MessageStatus.RECALL.code());
        updateById(msg);
        // 生成一条撤回消息
        GroupMessage recallMsg = new GroupMessage();
        recallMsg.setSendId(session.getUserId());
        recallMsg.setStatus(MessageStatus.UNSENT.code());
        recallMsg.setType(MessageType.RECALL.code());
        recallMsg.setGroupId(msg.getGroupId());
        recallMsg.setSendTime(new Date());
        recallMsg.setContent(id.toString());
        recallMsg.setSendNickname(member.getShowNickname());
        save(recallMsg);
        // 群发
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(msg.getGroupId());
        GroupMessageRespDTO msgInfo = BeanUtils.copyProperties(recallMsg, GroupMessageRespDTO.class);
        IMGroupMessage<GroupMessageRespDTO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvIds(userIds);
        sendMessage.setData(msgInfo);
        imClient.sendGroupMessage(sendMessage);
        log.info("撤回群聊消息，发送id:{},群聊id:{},内容:{}", session.getUserId(), msg.getGroupId(), msg.getContent());
        return msgInfo;
    }

    @Override
    public void pullOfflineMessage(Long minId) {
        UserSession session = SessionContext.getSession();
        if(!imClient.isOnline(session.getUserId())){
            throw new GlobalException("网络连接失败，无法拉取离线消息");
        }
        // 查询用户加入的群组
        List<GroupMember> members = groupMemberService.findByUserId(session.getUserId());
        Map<Long, GroupMember> groupMemberMap = CollStreamUtil.toIdentityMap(members, GroupMember::getGroupId);
        Set<Long> groupIds = groupMemberMap.keySet();
        if(CollectionUtil.isEmpty(groupIds)){
            // 关闭加载中标志
            sendLoadingMessage(false);
            return;
        }
        // 开启加载中标志
        sendLoadingMessage(true);
        // 只能拉取最近3个月的,最多拉取3000条
        int months = session.getTerminal().equals(IMTerminalType.APP.code()) ? 1 : 3;
        Date minDate = DateUtils.addMonths(new Date(), -months);
        LambdaQueryWrapper<GroupMessage> wp = Wrappers.lambdaQuery(GroupMessage.class)
                .gt(GroupMessage::getId, minId)
                .gt(GroupMessage::getSendTime, minDate)
                .in(GroupMessage::getGroupId, groupIds)
                .orderByAsc(GroupMessage::getId);
        List<GroupMessage> msgList = list(wp);
        // 通过群聊id对消息进行分组
        Map<Long, List<GroupMessage>> msgMap = msgList.stream().collect(Collectors.groupingBy(GroupMessage::getGroupId));
        // 拉取退群前的消息
        List<GroupMember> quitGroupMembers = groupMemberService.findQuitInMonth(session.getUserId());
        for (GroupMember quitMember : quitGroupMembers) {
            wp = Wrappers.lambdaQuery();
            wp.gt(GroupMessage::getId, minId)
                    .between(GroupMessage::getSendTime, minDate,quitMember.getQuitTime())
                    .eq(GroupMessage::getGroupId, quitMember.getGroupId())
                    .ne(GroupMessage::getStatus, MessageStatus.RECALL.code())
                    .orderByAsc(GroupMessage::getId);
            List<GroupMessage> groupMessages = list(wp);
            msgMap.put(quitMember.getGroupId(), groupMessages);
            groupMemberMap.put(quitMember.getGroupId(), quitMember);
        }
        // 推送消息
        AtomicInteger sendCount = new AtomicInteger();
        msgMap.forEach((groupId, groupMessages) -> {
            // 填充消息状态
            String key = StrUtil.join(":", RedisKey.IM_GROUP_READED_POSITION, groupId);
            Object o = redisTemplate.opsForHash().get(key, session.getUserId().toString());
            long readedMaxId = o == null ? -1 : Long.parseLong(o.toString());
            Map<Object, Object> maxIdMap = null;
            for(GroupMessage m : groupMessages){
                // 排除加群之前的消息
                GroupMember member = groupMemberMap.get(m.getGroupId());
                if(DateUtil.compare(member.getCreatedTime(), m.getSendTime()) > 0){
                    continue;
                }
                // 排除不需要接收的消息,recvIds非空同时不在recvIds里
                List<String> recvIds = CommaTextUtils.asList(m.getRecvIds());
                if(!recvIds.isEmpty() && !recvIds.contains(session.getUserId().toString())){
                    continue;
                }
                // 组装vo
                GroupMessageRespDTO vo = BeanUtils.copyProperties(m, GroupMessageRespDTO.class);
                // 被@用户列表
                List<String> atIds = CommaTextUtils.asList(m.getAtUserIds());
                vo.setAtUserIds(atIds.stream().map(Long::parseLong).collect(Collectors.toList()));
                // 填充状态,可能在其他客户端读过了
                vo.setStatus(readedMaxId >= m.getId() ? MessageStatus.READ.code() : MessageStatus.SENT.code());
                // 针对回执消息填充已读人数
                if(m.getReceipt()){
                    if(Objects.isNull(maxIdMap)) {
                        maxIdMap = redisTemplate.opsForHash().entries(key);
                    }
                    int count = getReadedUserIds(maxIdMap, m.getId(),m.getSendId()).size();
                    vo.setReadedCount(count);
                }
                // 推送
                IMGroupMessage<GroupMessageRespDTO> sendMessage = new IMGroupMessage<>();
                sendMessage.setSender(new IMUserInfo(m.getSendId(), IMTerminalType.WEB.code()));
                sendMessage.setRecvIds(Arrays.asList(session.getUserId()));
                sendMessage.setRecvTerminals(Arrays.asList(session.getTerminal()));
                sendMessage.setIsSendBack(false);
                sendMessage.setSendToSelf(false);
                sendMessage.setData(vo);
                imClient.sendGroupMessage(sendMessage);
                sendCount.getAndIncrement();
            }
        });
        // 关闭加载中标志
        sendLoadingMessage(false);
        log.info("拉取离线群聊消息,用户id:{},数量:{}",session.getUserId(),sendCount.get());
    }

    @Override
    public void readedMessage(Long groupId) {
        UserSession session = SessionContext.getSession();
        // 取出最后的消息id
        Long maxMsgId = groupMessageMapper.findLastByGroupId(groupId);
        if(maxMsgId == null){
            return;
        }
        // 推送消息给自己的其他终端,同步清空会话列表中的未读数量
        GroupMessageRespDTO msgInfo = new GroupMessageRespDTO();
        msgInfo.setType(MessageType.READED.code());
        msgInfo.setSendTime(new Date());
        msgInfo.setSendId(session.getUserId());
        msgInfo.setGroupId(groupId);
        IMGroupMessage<GroupMessageRespDTO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setSendToSelf(true);
        sendMessage.setData(msgInfo);
        sendMessage.setIsSendBack(true);
        imClient.sendGroupMessage(sendMessage);
        // 已读消息key
        String key = StrUtil.join(":", RedisKey.IM_GROUP_READED_POSITION, groupId);
        // 原来的已读消息位置
        Object oldMaxReadedId = redisTemplate.opsForHash().get(key, session.getUserId().toString());
        // 记录已读消息位置
        redisTemplate.opsForHash().put(key, String.valueOf(session.getUserId()),String.valueOf(maxMsgId));
        List<GroupMessage> unreadReceiptMsgs = groupMessageMapper.findUnreadReceiptMsg(groupId,oldMaxReadedId,maxMsgId,MessageStatus.RECALL.code(),true);
        if (CollectionUtil.isNotEmpty(unreadReceiptMsgs)) {
            List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
            Map<Object, Object> maxIdMap = redisTemplate.opsForHash().entries(key);
            for (GroupMessage receiptMessage : unreadReceiptMsgs) {
                Integer readedCount = getReadedUserIds(maxIdMap, receiptMessage.getId(),receiptMessage.getSendId()).size();
                // 如果所有人都已读，记录回执消息完成标记
                if(readedCount >= userIds.size() - 1){
                    receiptMessage.setReceiptOk(true);
                    this.updateById(receiptMessage);
                }
                msgInfo = new GroupMessageRespDTO();
                msgInfo.setId(receiptMessage.getId());
                msgInfo.setGroupId(groupId);
                msgInfo.setReadedCount(readedCount);
                msgInfo.setReceiptOk(receiptMessage.getReceiptOk());
                msgInfo.setType(MessageType.RECEIPT.code());
                sendMessage = new IMGroupMessage<>();
                sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
                sendMessage.setRecvIds(userIds);
                sendMessage.setData(msgInfo);
                sendMessage.setSendToSelf(false);
                sendMessage.setIsSendBack(false);
                imClient.sendGroupMessage(sendMessage);
            }
        }
    }

    @Override
    public List<Long> findReadedUsers(Long groupId, Long messageId) {
        UserSession session = SessionContext.getSession();
        GroupMessage message = this.getById(messageId);
        if (message == null) {
            throw new GlobalException("消息不存在");
        }
        // 是否在群聊里面
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, session.getUserId());
        if (member == null || member.getQuit()) {
            throw new GlobalException("您已不在群聊里面");
        }
        // 已读位置key
        String key = StrUtil.join(":", RedisKey.IM_GROUP_READED_POSITION, groupId);
        // 一次获取所有用户的已读位置
        Map<Object, Object> maxIdMap = redisTemplate.opsForHash().entries(key);
        // 返回已读用户的id集合
        return getReadedUserIds(maxIdMap, message.getId(),message.getSendId());
    }

    @Override
    public List<GroupMessageRespDTO> findHistoryMessage(Long groupId, Long page, Long size) {
        page = page > 0 ? page : 1;
        size = size > 0 ? size : 10;
        Long userId = SessionContext.getSession().getUserId();
        long offset = (page - 1) * size;
        // 群聊成员信息
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, userId);
        if (Objects.isNull(member) || member.getQuit()) {
            throw new GlobalException("您已不在群聊中");
        }
        // 查询聊天记录，只查询加入群聊时间之后的消息
        List<GroupMessage> messages = groupMessageMapper.findHistoryMsg(groupId,member.getCreatedTime(),MessageStatus.RECALL.code(),offset,size);
        List<GroupMessageRespDTO> msgVOs =
                messages.stream().map(m -> BeanUtils.copyProperties(m, GroupMessageRespDTO.class)).toList();
        log.info("拉取群聊记录，用户id:{},群聊id:{}，数量:{}", userId, groupId,msgVOs.size());
        return msgVOs;
    }

    /**
     * 获取已读用户Id,用于回执消息
     */
    private List<Long> getReadedUserIds(Map<Object, Object> maxIdMap, Long messageId, Long sendId) {
        List<Long> userIds = new LinkedList<>();
        maxIdMap.forEach((k, v) -> {
            Long userId = Long.valueOf(k.toString());
            long maxId = Long.parseLong(v.toString());
            // 发送者不计入已读人数
            if (!sendId.equals(userId) && maxId >= messageId) {
                userIds.add(userId);
            }
        });
        return userIds;
    }

    /**
     * 发送加载标志，前端根据这个判断是否将cacheChats刷新到chats里
     */
    private void sendLoadingMessage(Boolean isLoadding){
        log.info("拉取user:{}群聊消息状态:{}",SessionContext.getSession().getUserId(),isLoadding);
        UserSession session = SessionContext.getSession();
        GroupMessageRespDTO msgInfo = new GroupMessageRespDTO();
        msgInfo.setType(MessageType.LOADING.code());
        msgInfo.setContent(isLoadding.toString());
        IMGroupMessage sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvIds(Arrays.asList(session.getUserId()));
        sendMessage.setRecvTerminals(Arrays.asList(session.getTerminal()));
        sendMessage.setData(msgInfo);
        sendMessage.setSendToSelf(false);
        sendMessage.setIsSendBack(false);
        imClient.sendGroupMessage(sendMessage);
    }
}




