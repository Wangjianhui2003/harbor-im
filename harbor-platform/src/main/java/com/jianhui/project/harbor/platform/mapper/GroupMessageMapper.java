package com.jianhui.project.harbor.platform.mapper;

import com.jianhui.project.harbor.platform.entity.GroupMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.Date;
import java.util.List;

public interface GroupMessageMapper extends BaseMapper<GroupMessage> {

    /**
     * 查找group下消息最大Id
     */
    Long findLastByGroupId(Long groupId);

    List<GroupMessage> findUnreadReceiptMsg(Long groupId,Object oldMaxReadedId, Long maxMsgId, Integer recallCode, boolean isReceipt);

    List<GroupMessage> findHistoryMsg(Long groupId, Date createdTime, Integer recallCode, long offset, Long size);
}




