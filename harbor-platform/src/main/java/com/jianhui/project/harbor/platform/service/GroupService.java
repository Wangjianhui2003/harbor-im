package com.jianhui.project.harbor.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.entity.Group;
import com.jianhui.project.harbor.platform.pojo.vo.GroupInviteVO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupMemberVO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface GroupService extends IService<Group> {

    GroupVO createGroup(@Valid GroupVO vo);

    GroupVO modifyGroup(@Valid GroupVO vo);

    void deleteGroup(@NotNull(message = "群聊id不能为空") Long groupId);

    GroupVO findById(@NotNull(message = "群聊id不能为空") Long groupId);

    List<GroupVO> findGroups();

    void invite(@Valid GroupInviteVO vo);

    List<GroupMemberVO> findGroupMembers(@NotNull(message = "群聊id不能为空") Long groupId);

    void quitGroup(@NotNull(message = "群聊id不能为空") Long groupId);

    void kickGroup(@NotNull(message = "群聊id不能为空") Long groupId, @NotNull(message = "用户id不能为空") Long userId);
}
