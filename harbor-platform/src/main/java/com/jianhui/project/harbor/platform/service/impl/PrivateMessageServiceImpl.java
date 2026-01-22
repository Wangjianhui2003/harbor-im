package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.client.IMClient;
import com.jianhui.project.harbor.common.constant.IMConstant;
import com.jianhui.project.harbor.common.enums.IMTerminalType;
import com.jianhui.project.harbor.common.model.IMPrivateMessage;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.platform.dao.entity.PrivateMessage;
import com.jianhui.project.harbor.platform.dao.mapper.PrivateMessageMapper;
import com.jianhui.project.harbor.platform.dto.request.PrivateMessageDTO;
import com.jianhui.project.harbor.platform.dto.response.PrivateMessageRespDTO;
import com.jianhui.project.harbor.platform.enums.MessageStatus;
import com.jianhui.project.harbor.platform.enums.MessageType;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.service.FriendService;
import com.jianhui.project.harbor.platform.service.PrivateMessageService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class PrivateMessageServiceImpl extends ServiceImpl<PrivateMessageMapper, PrivateMessage> implements PrivateMessageService {

    private final FriendService friendService;
    private final IMClient imClient;
    private final PrivateMessageMapper privateMessageMapper;

    @Override
    public PrivateMessageRespDTO sendMessage(PrivateMessageDTO dto) {
        UserSession session = SessionContext.getSession();
        Boolean isFriend = friendService.isFriend(session.getUserId(), dto.getRecvId());
        if (Boolean.FALSE.equals(isFriend)) {
            throw new GlobalException("您已不是对方好友，无法发送消息");
        }
        //保存
        PrivateMessage msg = BeanUtils.copyProperties(dto, PrivateMessage.class);
        msg.setSendId(session.getUserId());
        msg.setStatus(MessageStatus.UNSENT.code());
        msg.setSendTime(new Date());
        save(msg);
        PrivateMessageRespDTO msgVO = BeanUtils.copyProperties(msg, PrivateMessageRespDTO.class);
        IMPrivateMessage<PrivateMessageRespDTO> imMsg = new IMPrivateMessage<>();
        imMsg.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        imMsg.setRecvId(msgVO.getRecvId());
        imMsg.setSendToSelf(true);
        imMsg.setData(msgVO);
        imMsg.setIsSendBack(true);
        imClient.sendPrivateMessage(imMsg);
        log.info("发送私聊消息，发送id:{},接收id:{}，内容:{}", session.getUserId(), dto.getRecvId(), dto.getContent());
        return msgVO;
    }

    @Override
    public List<PrivateMessageRespDTO> findHistoryMessage(Long friendId, Long page, Long size) {
        page = page > 0 ? page : 1;
        size = size > 0 ? size : 10;
        Long userId = SessionContext.getSession().getUserId();
        long offset = (page - 1) * size;
        //查询
        List<PrivateMessage> msgList = privateMessageMapper.pageHistoryMsg(userId, friendId, offset, size,MessageStatus.RECALL.code());
        //转为VO
        List<PrivateMessageRespDTO> msgVOList = msgList.stream()
                .map(m -> BeanUtils.copyProperties(m, PrivateMessageRespDTO.class)).toList();
        log.info("拉取聊天记录，用户id:{},好友id:{}，数量:{}", userId, friendId,msgVOList.size());
        return msgVOList;
    }

    @Override
    public void pullOfflineMessage(Long minId) {
        sendLoadingMessage(true);
        UserSession session = SessionContext.getSession();
        // 只能拉取最近3个月的消息,移动端只拉取一个月消息
        int months = session.getTerminal().equals(IMTerminalType.APP.code()) ? 1 : 3;
        Date minDate = DateUtils.addMonths(new Date(), -months);
        // 查询
        List<PrivateMessage> msgList = privateMessageMapper.getOfflineMsg(session.getUserId(),minId,minDate,MessageStatus.RECALL.code());
        // 推送
        for(PrivateMessage msg : msgList) {
            PrivateMessageRespDTO msgVO = BeanUtils.copyProperties(msg, PrivateMessageRespDTO.class);
            IMPrivateMessage<PrivateMessageRespDTO> sendMessage = new IMPrivateMessage<>();
            sendMessage.setSender(new IMUserInfo(msg.getSendId(), IMTerminalType.WEB.code()));
            sendMessage.setRecvId(session.getUserId());
            sendMessage.setRecvTerminals(List.of(session.getTerminal()));
            sendMessage.setSendToSelf(false);
            sendMessage.setData(msgVO);
            sendMessage.setIsSendBack(true);
            imClient.sendPrivateMessage(sendMessage);
        }
        sendLoadingMessage(false);
        log.info("拉取私聊消息，用户id:{},数量:{}", session.getUserId(),msgList.size());
    }

    @Transactional
    @Override
    public PrivateMessageRespDTO recallMessage(Long id) {
        UserSession session = SessionContext.getSession();
        PrivateMessage msg = getById(id);
        if (msg == null) {
            throw new GlobalException("消息不存在");
        }
        if (!msg.getSendId().equals(session.getUserId())) {
            throw new GlobalException("这条消息不是由您发送,无法撤回");
        }
        if(System.currentTimeMillis() - msg.getSendTime().getTime() > IMConstant.ALLOW_RECALL_SECOND * 1000){
            throw new GlobalException("消息已发送超过5分钟，无法撤回");
        }
        // 修改消息状态
        msg.setStatus(MessageStatus.RECALL.code());
        updateById(msg);
        // 生成一条撤回消息
        PrivateMessage recallMsg = new PrivateMessage();
        recallMsg.setSendId(session.getUserId());
        recallMsg.setStatus(MessageStatus.UNSENT.code());
        recallMsg.setSendTime(new Date());
        recallMsg.setRecvId(msg.getRecvId());
        recallMsg.setType(MessageType.RECALL.code());
        recallMsg.setContent(id.toString());
        save(recallMsg);
        // 推送消息
        PrivateMessageRespDTO msgInfo = BeanUtils.copyProperties(recallMsg, PrivateMessageRespDTO.class);
        IMPrivateMessage<PrivateMessageRespDTO> imMsg = new IMPrivateMessage<>();
        imMsg.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        imMsg.setRecvId(msgInfo.getRecvId());
        imMsg.setData(msgInfo);
        imClient.sendPrivateMessage(imMsg);
        log.info("撤回私聊消息，发送id:{},接收id:{}，内容:{}", msg.getSendId(), msg.getRecvId(), msg.getContent());
        return msgInfo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void readedMessage(Long friendId) {
        UserSession session = SessionContext.getSession();
        // 推送消息给自己，清空会话列表上的已读数量
        PrivateMessageRespDTO msgInfo = new PrivateMessageRespDTO();
        msgInfo.setType(MessageType.READED.code());
        msgInfo.setSendId(session.getUserId());
        msgInfo.setRecvId(friendId);
        IMPrivateMessage<PrivateMessageRespDTO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setData(msgInfo);
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setSendToSelf(true);
        sendMessage.setIsSendBack(false);
        imClient.sendPrivateMessage(sendMessage);
        // 推送回执消息给对方，更新已读状态
        msgInfo = new PrivateMessageRespDTO();
        msgInfo.setType(MessageType.RECEIPT.code());
        msgInfo.setSendId(session.getUserId());
        msgInfo.setRecvId(friendId);
        sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(friendId);
        sendMessage.setSendToSelf(false);
        sendMessage.setIsSendBack(false);
        sendMessage.setData(msgInfo);
        imClient.sendPrivateMessage(sendMessage);
        // 修改消息状态为已读
        privateMessageMapper.updateStatusToReaded(
                session.getUserId(),
                friendId,
                MessageStatus.SENT.code(),
                MessageStatus.READ.code());
        log.info("消息已读，接收方id:{},发送方id:{}", session.getUserId(), friendId);
    }

    @Override
    public Long getMaxReadedId(Long friendId) {
        UserSession session = SessionContext.getSession();
        Long maxId = privateMessageMapper.getMaxReadedMsgId(session.getUserId(), friendId, MessageStatus.READ.code());
        if(maxId == null) {
            return -1L;
        }
        return maxId;
    }

    /**
     * 发送加载标记true/false
     * 告诉前端现在正在推送离线消息，不要将cacheChats加载到chats里
     */
    private void sendLoadingMessage(Boolean isLoading) {
        log.info("私聊加载信号发送:{}", isLoading);
        UserSession session = SessionContext.getSession();
        PrivateMessageRespDTO msgInfo = new PrivateMessageRespDTO();
        msgInfo.setType(MessageType.LOADING.code());
        //加载标识
        msgInfo.setContent(isLoading.toString());
        IMPrivateMessage<PrivateMessageRespDTO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(session.getUserId());
        sendMessage.setRecvTerminals(List.of(session.getTerminal()));
        sendMessage.setData(msgInfo);
        sendMessage.setSendToSelf(false);
        sendMessage.setIsSendBack(false);
        imClient.sendPrivateMessage(sendMessage);
    }
}




