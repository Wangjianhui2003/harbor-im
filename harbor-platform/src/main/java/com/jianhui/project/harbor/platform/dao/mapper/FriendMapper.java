package com.jianhui.project.harbor.platform.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jianhui.project.harbor.platform.dao.entity.Friend;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface FriendMapper extends BaseMapper<Friend> {

    @Update("update t_friend set friend_nickname = #{nickname}, friend_head_image = #{thumb} "
            + "where friend_id = #{friendId}")
    void updateFriendNicknameAndThumb(Long friendId, String nickname, String thumb);

    @Select("select * from t_friend where user_id = #{userId}")
    List<Friend> findAllFriendsByUserId(Long userId);

    @Select("select * from t_friend where user_id = #{userId} and friend_id = #{friendId}")
    Friend getByUserIdAndFriendId(Long userId,Long friendId);

    @Update("update t_friend set deleted = #{b} where user_id = #{userId} and friend_id = #{friendId}")
    boolean setUnbind(Long userId, Long friendId, boolean b);

    /**
     * 更改好友昵称
     *
     * @param remark
     * @param userId
     * @param friendId
     * @return
     */
    int updateFriendNicknameByUserIdAndFriendId(@Param("remark") String remark, @Param("userId") Long userId, @Param("friendId") Long friendId);
}




