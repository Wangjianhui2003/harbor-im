package com.jianhui.project.harbor.platform.service;

import com.jianhui.project.harbor.platform.entity.Friend;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.pojo.resp.FriendResp;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface FriendService extends IService<Friend> {

    List<FriendResp> findFriends();

    void addFriend(@NotNull(message = "好友id不可为空") Long friendId);

    FriendResp findFriend(@NotNull(message = "好友id不可为空") Long friendId);

    void delFriend(@NotNull(message = "好友id不可为空") Long friendId);
}
