package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.entity.Group;
import com.jianhui.project.harbor.platform.pojo.vo.GroupInviteVO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupMemberVO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupVO;
import com.jianhui.project.harbor.platform.service.GroupService;
import com.jianhui.project.harbor.platform.mapper.GroupMapper;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group>
    implements GroupService{

    @Override
    public GroupVO createGroup(GroupVO vo) {
        return null;
    }

    @Override
    public GroupVO modifyGroup(GroupVO vo) {
        return null;
    }

    @Override
    public void deleteGroup(Long groupId) {

    }

    @Override
    public GroupVO findById(Long groupId) {
        return null;
    }

    @Override
    public List<GroupVO> findGroups() {
        return List.of();
    }

    @Override
    public void invite(GroupInviteVO vo) {

    }

    @Override
    public List<GroupMemberVO> findGroupMembers(Long groupId) {
        return List.of();
    }

    @Override
    public void quitGroup(Long groupId) {

    }

    @Override
    public void kickGroup(Long groupId, Long userId) {

    }
}




