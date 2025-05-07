package com.jianhui.project.harbor.platform.mapper;

import com.jianhui.project.harbor.platform.entity.Friend;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

public interface FriendMapper extends BaseMapper<Friend> {

    @Update("update t_friend set friend_nickname = #{nickname}, friend_head_image = #{thumb} "
            + "where friend_id = #{friendId}")
   void updateFriendNicknameAndThumb(Long friendId, String nickname, String thumb);

}




