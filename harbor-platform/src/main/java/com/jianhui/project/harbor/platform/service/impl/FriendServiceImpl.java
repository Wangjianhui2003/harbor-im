package com.jianhui.project.harbor.platform.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.client.IMClient;
import com.jianhui.project.harbor.common.enums.IMTerminalType;
import com.jianhui.project.harbor.common.model.IMPrivateMessage;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.platform.constant.RedisKey;
import com.jianhui.project.harbor.platform.dao.entity.Friend;
import com.jianhui.project.harbor.platform.dao.entity.PrivateMessage;
import com.jianhui.project.harbor.platform.dao.entity.User;
import com.jianhui.project.harbor.platform.dao.mapper.FriendMapper;
import com.jianhui.project.harbor.platform.dao.mapper.PrivateMessageMapper;
import com.jianhui.project.harbor.platform.dao.mapper.UserMapper;
import com.jianhui.project.harbor.platform.dto.response.FriendRespDTO;
import com.jianhui.project.harbor.platform.dto.response.PrivateMessageRespDTO;
import com.jianhui.project.harbor.platform.enums.MessageStatus;
import com.jianhui.project.harbor.platform.enums.MessageType;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.service.FriendService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @description: 好友服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = RedisKey.IM_CACHE_FRIEND)
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final IMClient imClient;
    private final PrivateMessageMapper privateMessageMapper;

    public List<Friend> findAllFriends() {
        //无VO
        Long userId = SessionContext.getSession().getUserId();
        return friendMapper.findAllFriendsByUserId(userId);
    }

    @Override
    public List<FriendRespDTO> findFriends() {
        List<Friend> allFriends = findAllFriends();
        return allFriends.stream().map(this::convertToFriendVO).toList();
    }

    @Override
    public List<Friend> findByFriendIds(List<Long> friendIds) {
        Long userId = SessionContext.getSession().getUserId();
        LambdaQueryWrapper<Friend> wrapper = Wrappers.lambdaQuery(Friend.class)
                .eq(Friend::getUserId, userId)
                .in(Friend::getFriendId, friendIds)
                .eq(Friend::getDeleted, false);
        return list(wrapper);
    }

    @Cacheable(key = "#userId1 + ':' + #userId2")
    @Override
    public Boolean isFriend(Long userId1, Long userId2) {
        LambdaQueryWrapper<Friend> wrapper = Wrappers.lambdaQuery(Friend.class)
                .eq(Friend::getUserId,userId1)
                .eq(Friend::getFriendId,userId2)
                .eq(Friend::getDeleted,false);
        return exists(wrapper);
    }

    @Override
    public FriendRespDTO findFriendInfo(Long friendId) {
        UserSession session = SessionContext.getSession();
        Friend friend = friendMapper.getByUserIdAndFriendId(session.getUserId(), friendId);
        if (friend == null) {
            throw new GlobalException("对方不是您的好友");
        }
        return convertToFriendVO(friend);
    }

    /**
     * 将Friend对象转换为FriendVO
     */
    private FriendRespDTO convertToFriendVO(Friend friend) {
        FriendRespDTO friendRespDTO = new FriendRespDTO();
        //将friendId作为FriendVO的id
        friendRespDTO.setId(friend.getFriendId());
        friendRespDTO.setFriendNickname(friend.getFriendNickname());
        friendRespDTO.setDeleted(friend.getDeleted());
        friendRespDTO.setHeadImage(friend.getFriendHeadImage());
        return friendRespDTO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addFriend(Long friendId) {
        long userId = SessionContext.getSession().getUserId();
        if (friendId.equals(userId)) {
            throw new GlobalException("不允许添加自己为好友");
        }
        //使用代理调用，避免aop(spring cache)失效
        FriendServiceImpl proxy = (FriendServiceImpl) AopContext.currentProxy();
        proxy.bindFriend(userId,friendId);
        proxy.bindFriend(friendId,userId);
        sendAddTipMessage(friendId);
        log.info("添加好友，用户id:{},好友id:{}", userId, friendId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delFriend(Long friendId) {
        Long userId = SessionContext.getSession().getUserId();
        // 互相解除好友关系，走代理清理缓存
        FriendServiceImpl proxy = (FriendServiceImpl)AopContext.currentProxy();
        proxy.unbindFriend(userId, friendId);
        proxy.unbindFriend(friendId, userId);
        // 推送解除好友提示
        sendDelTipMessage(friendId);
        log.info("删除好友，用户id:{},好友id:{}", userId, friendId);
    }

    @Override
    @CacheEvict(key = "#userId+':'+#friendId") //为什么要evict
    public void bindFriend(Long userId, Long friendId) {
        Friend friend = friendMapper.getByUserIdAndFriendId(userId, friendId);
        if (friend == null) {
            friend = new Friend();
        }
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        User friendInfo = userMapper.selectById(friendId);
        friend.setFriendHeadImage(friendInfo.getHeadImage());
        friend.setFriendNickname(friendInfo.getNickname());
        friend.setDeleted(false);
        saveOrUpdate(friend);
        sendAddFriendMessage(userId, friendId, friend);
    }

    @CacheEvict(key = "#userId + ':' + #friendId")
    public void unbindFriend(Long userId,Long friendId){
        // 逻辑删除
        boolean b = friendMapper.setUnbind(userId, friendId, true);
        // 推送好友变化信息
        sendDelFriendMessage(userId, friendId);
    }

    private void sendAddFriendMessage(Long userId, Long friendId, Friend friend) {
        // 推送好友添加信息
        PrivateMessageRespDTO msgInfo = new PrivateMessageRespDTO();
        msgInfo.setSendId(friendId); //TODO:这样反过来有点反逻辑，需要测试
        msgInfo.setRecvId(userId);
        msgInfo.setSendTime(new Date());
        msgInfo.setType(MessageType.FRIEND_NEW.code());
        FriendRespDTO vo = convertToFriendVO(friend);
        msgInfo.setContent(JSON.toJSONString(vo));
        IMPrivateMessage<PrivateMessageRespDTO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(friendId, IMTerminalType.UNKNOW.code()));
        sendMessage.setRecvId(userId);
        sendMessage.setData(msgInfo);
        sendMessage.setSendToSelf(false);
        sendMessage.setIsSendBack(false);
        imClient.sendPrivateMessage(sendMessage);
    }

    /**
     * 发送好友删除消息
     * @param userId
     * @param friendId
     */
    private void sendDelFriendMessage(Long userId, Long friendId) {
        // 推送好友状态信息
        PrivateMessageRespDTO msgInfo = new PrivateMessageRespDTO();
        msgInfo.setSendId(friendId);
        msgInfo.setRecvId(userId);
        msgInfo.setSendTime(new Date());
        msgInfo.setType(MessageType.FRIEND_DEL.code());
        IMPrivateMessage<PrivateMessageRespDTO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(friendId, IMTerminalType.UNKNOW.code()));
        sendMessage.setRecvId(userId);
        sendMessage.setData(msgInfo);
        sendMessage.setSendToSelf(false);
        sendMessage.setIsSendBack(false);
        imClient.sendPrivateMessage(sendMessage);
    }

    /**
     * 添加好友提示
     * @param friendId
     */
    private void sendAddTipMessage(Long friendId) {
        UserSession session = SessionContext.getSession();
        PrivateMessage msg = new PrivateMessage();
        msg.setSendId(session.getUserId());
        msg.setRecvId(friendId);
        msg.setStatus(MessageStatus.UNSENT.code());
        msg.setType(MessageType.TIP_TEXT.code());
        msg.setSendTime(new Date());
        msg.setContent("你们已成为好友，现在可以开始聊天了");
        privateMessageMapper.insert(msg);
        //推送到好友
        PrivateMessageRespDTO msgVO = BeanUtils.copyProperties(msg, PrivateMessageRespDTO.class);
        IMPrivateMessage<PrivateMessageRespDTO> imMsg = new IMPrivateMessage<>();
        imMsg.setData(msgVO);
        imMsg.setSender(new IMUserInfo(session.getUserId(),session.getTerminal()));
        imMsg.setRecvId(friendId);
        imMsg.setSendToSelf(false);
        imClient.sendPrivateMessage(imMsg);
        // 推给自己,这里不用SendToSelf是因为用户没有主动发这条信息，当前客户端是没有显示的，用imClient再发一次可以确保发到所有客户端
        imMsg.setRecvId(session.getUserId());
        imClient.sendPrivateMessage(imMsg);
    }

    /**
     * 发送删除好友提示信息
     * @param friendId
     */
    private void sendDelTipMessage(Long friendId){
        UserSession session = SessionContext.getSession();
        // 推送好友状态信息
        PrivateMessage msg = new PrivateMessage();
        msg.setSendId(session.getUserId());
        msg.setRecvId(friendId);
        msg.setSendTime(new Date());
        msg.setType(MessageType.TIP_TEXT.code());
        msg.setStatus(MessageStatus.UNSENT.code());
        msg.setContent("你们的好友关系已被解除");
        privateMessageMapper.insert(msg);
        // 推送
        PrivateMessageRespDTO messageInfo = BeanUtils.copyProperties(msg, PrivateMessageRespDTO.class);
        IMPrivateMessage<PrivateMessageRespDTO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(friendId, IMTerminalType.UNKNOW.code()));
        sendMessage.setRecvId(friendId);
        sendMessage.setData(messageInfo);
        imClient.sendPrivateMessage(sendMessage);
    }

    @Override
    public void editFriendRemarkName(FriendRespDTO friendRespDTO) {
        long userId = SessionContext.getSession().getUserId();
        int i = friendMapper.updateFriendNicknameByUserIdAndFriendId(friendRespDTO.getFriendNickname(), userId, friendRespDTO.getId());
        if (i != 1) {
            throw new GlobalException("更新好友昵称失败");
        }
    }
}




