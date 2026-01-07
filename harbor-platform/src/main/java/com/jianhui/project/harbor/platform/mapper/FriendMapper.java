package com.jianhui.project.harbor.platform.mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jianhui.project.harbor.platform.entity.Friend;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface FriendMapper extends BaseMapper<Friend> {

    /**
     * 将自己的变化更新到其他好友和群聊信息里
     * @param friendId
     * @param nickname
     * @param thumb
     */
    @Update("update t_friend set friend_nickname = #{nickname}, friend_head_image = #{thumb} "
            + "where friend_id = #{friendId}")
    void updateFriendNicknameAndThumb(Long friendId, String nickname, String thumb);

    @Select("select * from t_friend where user_id = #{userId} and deleted=0")
    List<Friend> findAllFriendsByUserId(Long userId);

    @Select("select * from t_friend where user_id = #{userId} and friend_id = #{friendId}")
    Friend getByUserIdAndFriendId(Long userId,Long friendId);

    @Update("update t_friend set deleted = #{b} where user_id = #{userId} and friend_id = #{friendId}")
    boolean setUnbind(Long userId, Long friendId, boolean b);

    /**
     * 更改好友昵称
     * @param remark
     * @param userId
     * @param friendId
     * @return
     */
    int updateFriendNicknameByUserIdAndFriendId(@Param("remark") String remark, @Param("userId") Long userId, @Param("friendId") Long friendId);
}




