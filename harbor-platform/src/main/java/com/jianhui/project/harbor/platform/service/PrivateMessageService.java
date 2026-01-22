package com.jianhui.project.harbor.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.dao.entity.PrivateMessage;
import com.jianhui.project.harbor.platform.dto.request.PrivateMessageDTO;
import com.jianhui.project.harbor.platform.dto.response.PrivateMessageRespDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
* @author wjh2
* @description 针对表【t_private_message(私聊消息)】的数据库操作Service
* @createDate 2025-05-04 18:25:04
*/
public interface PrivateMessageService extends IService<PrivateMessage> {

    /**
     * 发送私聊消息
     */
    PrivateMessageRespDTO sendMessage(@Valid PrivateMessageDTO dto);

    /**
     * 撤销消息
     */
    PrivateMessageRespDTO recallMessage(@NotNull(message = "消息id不能为空") Long id);

    /**
     * 拉取离线消息
     */
    void pullOfflineMessage(Long minId);

    /**
     * 已读消息
     */
    void readedMessage(Long friendId);

    /**
     * 获取会话已读信息的最大id
     */
    Long getMaxReadedId(Long friendId);

    /**
     * 查询历史消息
     */
    List<PrivateMessageRespDTO> findHistoryMessage(@NotNull(message = "好友id不能为空") Long friendId, @NotNull(message = "页码不能为空") Long page, @NotNull(message = "size不能为空") Long size);
}
