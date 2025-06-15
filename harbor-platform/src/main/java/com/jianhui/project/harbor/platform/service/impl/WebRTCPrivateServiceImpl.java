package com.jianhui.project.harbor.platform.service.impl;

import com.jianhui.project.harbor.client.IMClient;
import com.jianhui.project.harbor.common.constant.IMConstant;
import com.jianhui.project.harbor.common.model.IMPrivateMessage;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.platform.constant.RedisKey;
import com.jianhui.project.harbor.platform.entity.PrivateMessage;
import com.jianhui.project.harbor.platform.enums.MessageStatus;
import com.jianhui.project.harbor.platform.enums.MessageType;
import com.jianhui.project.harbor.platform.enums.WebRTCMode;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.pojo.vo.PrivateMessageVO;
import com.jianhui.project.harbor.platform.service.PrivateMessageService;
import com.jianhui.project.harbor.platform.service.WebRTCPrivateService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import com.jianhui.project.harbor.platform.session.WebRTCPrivateSession;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import com.jianhui.project.harbor.platform.util.UserStateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebRTCPrivateServiceImpl implements WebRTCPrivateService {

    private final IMClient imClient;
    private final PrivateMessageService privateMessageService;
    private final UserStateUtils userStateUtils;
    private final RedisTemplate<String,Object> redisTemplate;

    @Override
    public void call(Long uid, String mode, String offer) {
        UserSession session = SessionContext.getSession();
        log.info("发起呼叫,sid:{},uid:{}", session.getUserId(), uid);
        // 创建webrtc会话
        WebRTCPrivateSession webRTCPrivateSession = new WebRTCPrivateSession();
        webRTCPrivateSession.setCallerId(session.getUserId());
        webRTCPrivateSession.setCallerTerminal(session.getTerminal());
        webRTCPrivateSession.setAcceptorId(uid);
        webRTCPrivateSession.setMode(mode);
        //校验
        if (!imClient.isOnline(uid)){
            sendActMessage(webRTCPrivateSession,MessageStatus.UNSEND,"未接通(未在线)");
            log.info("RTC通话对方不在线:uid:{}", uid);
            throw new GlobalException("用户未上线");
        }
        if (userStateUtils.isBusy(uid)){
            sendActMessage(webRTCPrivateSession,MessageStatus.UNSEND,"未接通(忙线)");
            log.info("忙线中:uid:{}", uid);
            throw new GlobalException("用户忙线:" + uid);
        }
        //存rtc session
        String key = getWebRTCSessionKey(session.getUserId(), uid);
        redisTemplate.opsForValue().set(key,webRTCPrivateSession, IMConstant.RTC_PRIVATE_SESSION_TIMEOUT,TimeUnit.SECONDS);

        //设置用户忙线状态
        userStateUtils.setBusy(uid);
        userStateUtils.setBusy(session.getUserId());

        //给对方所有设备发offer消息
        PrivateMessageVO msgVO = new PrivateMessageVO();
        MessageType messageType = mode.equals(WebRTCMode.VIDEO.getValue()) ? MessageType.RTC_CALL_VIDEO : MessageType.RTC_CALL_VOICE;
        msgVO.setSendId(session.getUserId());
        msgVO.setRecvId(uid);
        msgVO.setContent(offer);
        msgVO.setType(messageType.code());
        IMPrivateMessage<PrivateMessageVO> imMsg = new IMPrivateMessage<>();
        imMsg.setSender(new IMUserInfo(session.getUserId(),session.getTerminal()));
        imMsg.setRecvId(uid);
        imMsg.setSendToSelf(false);
        imMsg.setIsSendBack(false);
        imMsg.setData(msgVO);
        imClient.sendPrivateMessage(imMsg);
    }

    @Override
    public void accept(Long uid, String answer) {
        UserSession session = SessionContext.getSession();
        //更新session信息
        WebRTCPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        webrtcSession.setAcceptorTerminal(session.getTerminal());
        webrtcSession.setChatTimeStamp(System.currentTimeMillis());
        String key = getWebRTCSessionKey(session.getUserId(), uid);
        redisTemplate.opsForValue().set(key, webrtcSession,IMConstant.RTC_PRIVATE_SESSION_TIMEOUT, TimeUnit.SECONDS);

        // 向发起人推送接受通话信令(answer)
        PrivateMessageVO msgVO = new PrivateMessageVO();
        msgVO.setSendId(session.getUserId());
        msgVO.setRecvId(uid);
        msgVO.setContent(answer);
        msgVO.setType(MessageType.RTC_ACCEPT.code());

        IMPrivateMessage<PrivateMessageVO> imMsg = new IMPrivateMessage<>();
        imMsg.setSender(new IMUserInfo(session.getUserId(),session.getTerminal()));
        imMsg.setRecvId(uid);
        //告诉自己其他终端已接收
        imMsg.setSendToSelf(true);
        imMsg.setIsSendBack(false);
        //发给一个终端就行
        imMsg.setRecvTerminals(List.of(webrtcSession.getCallerTerminal()));
        imMsg.setData(msgVO);
        imClient.sendPrivateMessage(imMsg);
    }

    @Override
    public void reject(Long uid) {
        UserSession session = SessionContext.getSession();
        log.info("拒绝通话,senderId:{},receiverId:{}", session.getUserId(), uid);
        //移除会话信息
        WebRTCPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        removeWebrtcSession(session.getUserId(), uid);
        //设置空闲状态
        userStateUtils.setFree(uid);
        userStateUtils.setFree(session.getUserId());
        //发消息
        PrivateMessageVO messageInfo = new PrivateMessageVO();
        messageInfo.setType(MessageType.RTC_REJECT.code());
        messageInfo.setRecvId(uid);
        messageInfo.setSendId(session.getUserId());

        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(uid);
        // 告知其他终端已经拒绝会话,中止呼叫
        sendMessage.setSendToSelf(true);
        sendMessage.setIsSendBack(false);
        sendMessage.setRecvTerminals(Collections.singletonList(webrtcSession.getCallerTerminal()));
        sendMessage.setData(messageInfo);
        imClient.sendPrivateMessage(sendMessage);
        //发送通话信息
        sendActMessage(webrtcSession,MessageStatus.UNSEND,"已拒绝");
    }

    @Override
    public void cancel(Long uid) {
        UserSession session = SessionContext.getSession();
        log.info("取消通话,senderId:{},receiverId:{}", session.getUserId(), uid);
        // 删除会话信息
        WebRTCPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        removeWebrtcSession(session.getUserId(), uid);
        // 设置用户空闲状态
        userStateUtils.setFree(uid);
        userStateUtils.setFree(session.getUserId());
        // 向对方所有终端推送取消通话信令
        PrivateMessageVO msgVO = new PrivateMessageVO();
        msgVO.setSendId(session.getUserId());
        msgVO.setRecvId(uid);
        msgVO.setType(MessageType.RTC_CANCEL.code());

        IMPrivateMessage<PrivateMessageVO> imMsg = new IMPrivateMessage<>();
        imMsg.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        imMsg.setRecvId(uid);
        imMsg.setSendToSelf(false);
        imMsg.setIsSendBack(false);
        imMsg.setData(msgVO);
        // 通知对方取消会话
        imClient.sendPrivateMessage(imMsg);
        // 生成通话消息
        sendActMessage(webrtcSession, MessageStatus.UNSEND, "已取消通话");
    }

    @Override
    public void failed(Long uid, String reason) {
        UserSession session = SessionContext.getSession();
        log.info("通话失败,sid:{},uid:{},reason:{}", session.getUserId(), uid, reason);
        // 删除会话信息
        WebRTCPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        removeWebrtcSession(uid, session.getUserId());
        // 设置用户空闲状态
        userStateUtils.setFree(uid);
        userStateUtils.setFree(session.getUserId());
        // 向发起方推送通话失败信令
        PrivateMessageVO messageInfo = new PrivateMessageVO();
        messageInfo.setSendId(session.getUserId());
        messageInfo.setRecvId(uid);
        messageInfo.setType(MessageType.RTC_FAILED.code());
        messageInfo.setContent(reason);

        // 通知对方取消会话
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(uid);
        sendMessage.setSendToSelf(false);
        sendMessage.setIsSendBack(false);
        sendMessage.setRecvTerminals(Collections.singletonList(webrtcSession.getCallerTerminal()));
        sendMessage.setData(messageInfo);
        imClient.sendPrivateMessage(sendMessage);

        sendActMessage(webrtcSession, MessageStatus.READED, "未接通(通话失败)");
    }

    @Override
    public void hangup(Long uid) {
        UserSession session = SessionContext.getSession();
        log.info("挂断通话,sid:{},uid:{}", session.getUserId(), uid);
        // 删除会话信息
        WebRTCPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        removeWebrtcSession(uid, session.getUserId());
        // 设置用户空闲状态
        userStateUtils.setFree(uid);
        userStateUtils.setFree(session.getUserId());
        // 向对方推送挂断通话信令
        PrivateMessageVO messageInfo = new PrivateMessageVO();
        messageInfo.setSendId(session.getUserId());
        messageInfo.setRecvId(uid);
        messageInfo.setType(MessageType.RTC_HANGUP.code());

        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(uid);
        sendMessage.setSendToSelf(false);
        sendMessage.setIsSendBack(false);
        Integer terminal = getTerminalType(uid, webrtcSession);
        sendMessage.setRecvTerminals(Collections.singletonList(terminal));
        sendMessage.setData(messageInfo);
        imClient.sendPrivateMessage(sendMessage);

        sendActMessage(webrtcSession, MessageStatus.READED, "通话时长 " + chatTimeText(webrtcSession));
    }

    @Override
    public void candidate(Long uid, String candidate) {
        UserSession session = SessionContext.getSession();
        // 查询webrtc会话
        WebRTCPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        // 向发起方推送同步candidate信令
        PrivateMessageVO messageInfo = new PrivateMessageVO();
        messageInfo.setSendId(session.getUserId());
        messageInfo.setRecvId(uid);
        messageInfo.setType(MessageType.RTC_CANDIDATE.code());
        messageInfo.setContent(candidate);

        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(uid);
        Integer terminal = getTerminalType(uid, webrtcSession);
        sendMessage.setRecvTerminals(Collections.singletonList(terminal));
        sendMessage.setSendToSelf(false);
        sendMessage.setIsSendBack(false);
        sendMessage.setData(messageInfo);
        imClient.sendPrivateMessage(sendMessage);
    }

    @Override
    public void heartbeat(Long uid) {
        UserSession session = SessionContext.getSession();
        // 会话续命
        String key = getWebRTCSessionKey(session.getUserId(), uid);
        redisTemplate.expire(key, IMConstant.RTC_PRIVATE_SESSION_TIMEOUT, TimeUnit.SECONDS);
        // 用户状态续命
        userStateUtils.expire(session.getUserId());
    }

    /**
     * 给双方发送通话消息
     * @param rtcSession 会话
     * @param status 信息状态 （有时候可以直接设置为已读）
     * @param content 内容
     */
    private void sendActMessage(WebRTCPrivateSession rtcSession, MessageStatus status, String content) {
        //判断通话类型
        MessageType messageType = rtcSession.getMode().equals(WebRTCMode.VIDEO.getValue()) ? MessageType.ACT_RT_VIDEO : MessageType.ACT_RT_VOICE;
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.setSendId(rtcSession.getCallerId());
        privateMessage.setRecvId(rtcSession.getAcceptorId());
        privateMessage.setContent(content);
        privateMessage.setStatus(status.code());
        privateMessage.setSendTime(new Date());
        privateMessage.setType(messageType.code());
        privateMessageService.save(privateMessage);

        PrivateMessageVO msgVO = BeanUtils.copyProperties(privateMessage, PrivateMessageVO.class);
        IMPrivateMessage<PrivateMessageVO> imMsg = new IMPrivateMessage<>();
        //推给发送方
        imMsg.setSender(new IMUserInfo(rtcSession.getCallerId(),rtcSession.getCallerTerminal()));
        imMsg.setRecvId(rtcSession.getCallerId());
        imMsg.setIsSendBack(true);
        imMsg.setSendToSelf(false);
        imMsg.setData(msgVO);
        imClient.sendPrivateMessage(imMsg);
        //推给接听方
        imMsg.setRecvId(rtcSession.getAcceptorId());
        imClient.sendPrivateMessage(imMsg);
    }

    /**
     * 获取webrtc session key字符串
     */
    private String getWebRTCSessionKey(Long id1, Long id2) {
        Long minId = id1 > id2 ? id2 : id1;
        Long maxId = id1 > id2 ? id1 : id2;
        return String.join(":", RedisKey.IM_WEBRTC_PRIVATE_SESSION, minId.toString(), maxId.toString());
    }

    /**
     * 获取webrtc session
     */
    private WebRTCPrivateSession getWebrtcSession(Long userId, Long uid) {
        String key = getWebRTCSessionKey(userId, uid);
        WebRTCPrivateSession webrtcSession = (WebRTCPrivateSession) redisTemplate.opsForValue().get(key);
        if (webrtcSession == null) {
            throw new GlobalException("通话已结束");
        }
        return webrtcSession;
    }

    /**
     * 获取rtc通话用户客户端
     */
    private Integer getTerminalType(Long uid, WebRTCPrivateSession webrtcSession) {
        if (uid.equals(webrtcSession.getCallerId())) {
            return webrtcSession.getCallerTerminal();
        }
        return webrtcSession.getAcceptorTerminal();
    }

    /**
     * 移除会话信息
     */
    private void removeWebrtcSession(Long userId, Long uid) {
        String key = getWebRTCSessionKey(userId, uid);
        redisTemplate.delete(key);
    }

    /**
     * 生成通话时长字符串
     */
    private String chatTimeText(WebRTCPrivateSession rtcSession) {
        long chatTime = (System.currentTimeMillis() - rtcSession.getChatTimeStamp()) / 1000;
        int min = Math.abs((int)chatTime / 60);
        int sec = Math.abs((int)chatTime % 60);
        String strTime = min < 10 ? "0" : "";
        strTime += min;
        strTime += ":";
        strTime += sec < 10 ? "0" : "";
        strTime += sec;
        return strTime;
    }
}
