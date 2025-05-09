package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.entity.GroupMessage;
import com.jianhui.project.harbor.platform.pojo.dto.GroupMessageDTO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupMessageVO;
import com.jianhui.project.harbor.platform.service.GroupMessageService;
import com.jianhui.project.harbor.platform.mapper.GroupMessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupMessageServiceImpl extends ServiceImpl<GroupMessageMapper, GroupMessage>
    implements GroupMessageService{

    @Override
    public GroupMessageVO sendMessage(GroupMessageDTO vo) {
        return null;
    }

    @Override
    public GroupMessageVO recallMessage(Long id) {
        return null;
    }

    @Override
    public void pullOfflineMessage(Long minId) {

    }

    @Override
    public void readedMessage(Long groupId) {

    }

    @Override
    public List<Long> findReadedUsers(Long groupId, Long messageId) {
        return List.of();
    }

    @Override
    public List<GroupMessageVO> findHistoryMessage(Long groupId, Long page, Long size) {
        return List.of();
    }
}




