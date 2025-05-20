package com.jianhui.project.harbor.platform.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.client.IMClient;
import com.jianhui.project.harbor.common.enums.IMTerminalType;
import com.jianhui.project.harbor.common.model.IMPrivateMessage;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.platform.entity.Friend;
import com.jianhui.project.harbor.platform.entity.PrivateMessage;
import com.jianhui.project.harbor.platform.entity.User;
import com.jianhui.project.harbor.platform.enums.MessageStatus;
import com.jianhui.project.harbor.platform.enums.MessageType;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.mapper.FriendMapper;
import com.jianhui.project.harbor.platform.mapper.PrivateMessageMapper;
import com.jianhui.project.harbor.platform.mapper.UserMapper;
import com.jianhui.project.harbor.platform.pojo.resp.FriendVO;
import com.jianhui.project.harbor.platform.pojo.vo.PrivateMessageVO;
import com.jianhui.project.harbor.platform.service.FriendService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @description: 好友服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
    public List<FriendVO> findFriends() {
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
    public FriendVO findFriendInfo(Long friendId) {
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
    private FriendVO convertToFriendVO(Friend friend) {
        FriendVO friendVO = new FriendVO();
        friendVO.setId(friend.getId());
        friendVO.setNickname(friend.getFriendNickname());
        friendVO.setDeleted(friend.getDeleted());
        friendVO.setHeadImage(friend.getFriendHeadImage());
        return friendVO;
    }

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

    @Override
    public void delFriend(Long friendId) {
        long userId = SessionContext.getSession().getUserId();
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
        PrivateMessageVO msgInfo = new PrivateMessageVO();
        msgInfo.setSendId(friendId); //TODO:这样反过来有点反逻辑，需要测试
        msgInfo.setRecvId(userId);
        msgInfo.setSendTime(new Date());
        msgInfo.setType(MessageType.FRIEND_NEW.code());
        FriendVO vo = convertToFriendVO(friend);
        msgInfo.setContent(JSON.toJSONString(vo));
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(friendId, IMTerminalType.UNKNOW.code()));
        sendMessage.setRecvId(userId);
        sendMessage.setData(msgInfo);
        sendMessage.setSendToSelf(false);
        sendMessage.setIsSendBack(false);
        imClient.sendPrivateMessage(sendMessage);
    }

    private void sendDelFriendMessage(Long userId, Long friendId) {
        // 推送好友状态信息
        PrivateMessageVO msgInfo = new PrivateMessageVO();
        msgInfo.setSendId(friendId);
        msgInfo.setRecvId(userId);
        msgInfo.setSendTime(new Date());
        msgInfo.setType(MessageType.FRIEND_DEL.code());
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(friendId, IMTerminalType.UNKNOW.code()));
        sendMessage.setRecvId(userId);
        sendMessage.setData(msgInfo);
        sendMessage.setSendToSelf(false);
        sendMessage.setIsSendBack(false);
        imClient.sendPrivateMessage(sendMessage);
    }

    private void sendAddTipMessage(Long friendId) {
        UserSession session = SessionContext.getSession();
        PrivateMessage msg = new PrivateMessage();
        msg.setSendId(session.getUserId());
        msg.setRecvId(friendId);
        msg.setStatus(MessageStatus.UNSEND.code());
        msg.setType(MessageType.TIP_TEXT.code());
        msg.setSendTime(new Date());
        msg.setContent("你们已成为好友，现在可以开始聊天了");
        privateMessageMapper.insert(msg);
        //推送到好友
        PrivateMessageVO msgVO = BeanUtils.copyProperties(msg, PrivateMessageVO.class);
        IMPrivateMessage<PrivateMessageVO> imMsg = new IMPrivateMessage<>();
        imMsg.setData(msgVO);
        imMsg.setSender(new IMUserInfo(session.getUserId(),session.getTerminal()));
        imMsg.setRecvId(friendId);
        imMsg.setSendToSelf(false);
        imClient.sendPrivateMessage(imMsg);
        // 推给自己,这里不用SendToSelf是因为用户没有主动发这条信息，当前客户端是没有显示的，用imClient再发一次可以确保发到所有客户端
        imMsg.setRecvId(session.getUserId());
        imClient.sendPrivateMessage(imMsg);
    }
}




