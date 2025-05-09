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

    PrivateMessageVO sendMessage(@Valid PrivateMessageDTO vo);

    PrivateMessageVO recallMessage(@NotNull(message = "消息id不能为空") Long id);

    void pullOfflineMessage(Long minId);

    void readedMessage(Long friendId);

    String getMaxReadedId(Long friendId);

    List<PrivateMessageVO> findHistoryMessage(@NotNull(message = "好友id不能为空") Long friendId, @NotNull(message = "页码不能为空") Long page, @NotNull(message = "size不能为空") Long size);
}
