package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.constant.RedisKey;
import com.jianhui.project.harbor.platform.dao.entity.GroupMember;
import com.jianhui.project.harbor.platform.dao.mapper.GroupMemberMapper;
import com.jianhui.project.harbor.platform.service.GroupMemberService;
import com.jianhui.project.harbor.platform.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@CacheConfig(cacheNames = RedisKey.IM_CACHE_GROUP_MEMBER_ID)
@RequiredArgsConstructor
@Slf4j
public class GroupMemberServiceImpl extends ServiceImpl<GroupMemberMapper, GroupMember>
    implements GroupMemberService{

    private final GroupMemberMapper groupMemberMapper;

    /**
     * 保存同时让缓存失效
     */
    @CacheEvict(key = "#member.getGroupId()")
    @Override
    public boolean save(GroupMember member) {
        return super.save(member);
    }

    /**
     * 更新或保存让缓存失效
     */
    @CacheEvict(key = "#groupId")
    @Override
    public boolean saveOrUpdateBatch(Long groupId, List<GroupMember> members) {
        return super.saveOrUpdateBatch(members);
    }

    @Override
    public GroupMember findByGroupAndUserId(Long groupId, Long userId) {
        return groupMemberMapper.getOneByGroupIdAndUserId(groupId,userId);
    }

    @Override
    public List<GroupMember> findByUserId(Long userId) {
        return groupMemberMapper.getByUserIdAndQuit(userId,false);
    }

    @Override
    public List<GroupMember> findQuitInMonth(Long userId) {
        Date date = new Date();
        //一个月前
        DateTimeUtils.addMonths(date, -1);
        return groupMemberMapper.findByUserIdAndQuitAndQuitTimeAfter(userId,true,date);
    }

    @Override
    public List<GroupMember> findByGroupId(Long groupId) {
        return groupMemberMapper.getByGroupId(groupId);
    }

    @Cacheable(key = "#groupId")
    @Override
    public List<Long> findUserIdsByGroupId(Long groupId) {
        return groupMemberMapper.findUserIdByGroupIdAndQuit(groupId,false);
    }

    @CacheEvict(key = "#groupId")
    @Override
    public void removeByGroupId(Long groupId) {
        int num = groupMemberMapper.updateQuitAndQuitTimeByGroupId(true,new Date(),groupId);
        log.debug("从群移除(逻辑):groupId:{},群成员数量:{}",groupId,num);
    }

    @CacheEvict(key = "#groupId")
    @Override
    public void removeByGroupAndUserId(Long groupId, Long userId) {
        groupMemberMapper.updateQuitAndQuitTimeByGroupIdAndUserId(true,new Date(),groupId,userId);
        log.debug("逻辑移除群成员:groupId:{},userId:{}",groupId,userId);
    }

    @Override
    public Boolean isInGroup(Long groupId, List<Long> userIds) {
        //TODO:isInGroup
        return null;
    }
}




