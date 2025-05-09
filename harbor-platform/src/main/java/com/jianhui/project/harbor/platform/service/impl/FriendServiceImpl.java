package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.entity.Friend;
import com.jianhui.project.harbor.platform.mapper.FriendMapper;
import com.jianhui.project.harbor.platform.pojo.resp.FriendResp;
import com.jianhui.project.harbor.platform.service.FriendService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description: 好友服务实现类
 */
@Service
@RequiredArgsConstructor
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    private final FriendMapper friendMapper;

    @Override
    public List<FriendResp> findFriends() {
        List<Friend> allFriends = findAllFriends();
        return allFriends.stream().map(friend -> convertToFriendResp(friend)).toList();
    }

    /**
     * 返回该user的所有好友
     */
    private List<Friend> findAllFriends() {
        Long userId = SessionContext.getSession().getUserId();
        return friendMapper.findAllFriendsByUserId(userId);
    }

    /**
     * 将Friend对象转换为FriendResp对象
     */
    private FriendResp convertToFriendResp(Friend friend) {
        FriendResp friendResp = new FriendResp();
        friendResp.setId(friend.getId());
        friendResp.setNickname(friend.getFriendNickname());
        friendResp.setDeleted(friend.getDeleted());
        friendResp.setHeadImage(friend.getFriendHeadImage());
        return friendResp;
    }

    @Override
    public void addFriend(Long friendId) {

    }

    @Override
    public FriendResp findFriend(Long friendId) {
        return null;
    }

    @Override
    public void delFriend(Long friendId) {

    }
}




