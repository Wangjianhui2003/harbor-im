package com.jianhui.project.harbor.platform.service;

import com.jianhui.project.harbor.platform.entity.PrivateMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.pojo.dto.PrivateMessageDTO;
import com.jianhui.project.harbor.platform.pojo.vo.PrivateMessageVO;
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
    PrivateMessageVO sendMessage(@Valid PrivateMessageDTO dto);

    /**
     * 撤销消息
     */
    PrivateMessageVO recallMessage(@NotNull(message = "消息id不能为空") Long id);

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
    List<PrivateMessageVO> findHistoryMessage(@NotNull(message = "好友id不能为空") Long friendId, @NotNull(message = "页码不能为空") Long page, @NotNull(message = "size不能为空") Long size);
}
