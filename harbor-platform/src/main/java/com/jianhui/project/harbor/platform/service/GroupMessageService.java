package com.jianhui.project.harbor.platform.service;

import com.jianhui.project.harbor.platform.entity.GroupMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.pojo.dto.GroupMessageDTO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupMessageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface GroupMessageService extends IService<GroupMessage> {

    GroupMessageVO sendMessage(@Valid GroupMessageDTO vo);

    GroupMessageVO recallMessage(@NotNull(message = "消息id不能为空") Long id);

    void pullOfflineMessage(Long minId);

    void readedMessage(Long groupId);

    List<Long> findReadedUsers(Long groupId, Long messageId);

    List<GroupMessageVO> findHistoryMessage(@NotNull(message = "群聊id不能为空") Long groupId, @NotNull(message = "页码不能为空") Long page, @NotNull(message = "size不能为空") Long size);
}
