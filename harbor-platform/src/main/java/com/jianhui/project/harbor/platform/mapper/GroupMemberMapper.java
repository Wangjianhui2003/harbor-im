package com.jianhui.project.harbor.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jianhui.project.harbor.platform.entity.GroupMember;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    GroupMember getOneByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    List<GroupMember> getByUserIdAndQuit(@Param("userId") Long userId, @Param("quit") Boolean quit);

    List<GroupMember> findByUserIdAndQuitAndQuitTimeAfter(@Param("userId") Long userId, @Param("quit") Boolean quit, @Param("quitTime") Date quitTime);

    List<GroupMember> getByGroupId(@Param("groupId") Long groupId);

    List<Long> findUserIdByGroupIdAndQuit(@Param("groupId") Long groupId, @Param("quit") Boolean quit);

    int deleteByGroupId(@Param("groupId") Long groupId);

    int updateQuitAndQuitTimeByGroupId(@Param("quit") Boolean quit, @Param("quitTime") Date quitTime, @Param("groupId") Long groupId);

    int updateQuitAndQuitTimeByGroupIdAndUserId(@Param("quit") Boolean quit, @Param("quitTime") Date quitTime, @Param("groupId") Long groupId, @Param("userId") Long userId);
}




