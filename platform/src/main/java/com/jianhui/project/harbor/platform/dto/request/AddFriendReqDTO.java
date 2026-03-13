package com.jianhui.project.harbor.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加好友请求DTO
 */
@Data
public class AddFriendReqDTO {

    /**
     * 接收方用户ID
     */
    @NotNull(message = "接收方用户ID不能为空")
    private Long receiveUserId;

    /**
     * 请求留言
     */
    private String requestNote;
}
