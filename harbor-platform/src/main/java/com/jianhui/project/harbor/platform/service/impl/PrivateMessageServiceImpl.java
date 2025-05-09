package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.entity.PrivateMessage;
import com.jianhui.project.harbor.platform.pojo.dto.PrivateMessageDTO;
import com.jianhui.project.harbor.platform.pojo.vo.PrivateMessageVO;
import com.jianhui.project.harbor.platform.service.PrivateMessageService;
import com.jianhui.project.harbor.platform.mapper.PrivateMessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrivateMessageServiceImpl extends ServiceImpl<PrivateMessageMapper, PrivateMessage>
    implements PrivateMessageService{

    @Override
    public PrivateMessageVO sendMessage(PrivateMessageDTO vo) {
        return null;
    }

    @Override
    public PrivateMessageVO recallMessage(Long id) {
        return null;
    }

    @Override
    public void pullOfflineMessage(Long minId) {

    }

    @Override
    public void readedMessage(Long friendId) {

    }

    @Override
    public String getMaxReadedId(Long friendId) {
        return "";
    }

    @Override
    public List<PrivateMessageVO> findHistoryMessage(Long friendId, Long page, Long size) {
        return List.of();
    }
}




