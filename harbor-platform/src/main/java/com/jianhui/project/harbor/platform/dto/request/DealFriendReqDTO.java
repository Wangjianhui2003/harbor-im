package com.jianhui.project.harbor.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 处理好友请求DTO
 */
@Data
public class DealFriendReqDTO {

    /**
     * 请求ID
     */
    @NotNull(message = "请求ID不能为空")
    private Long id;

    /**
     * 发起请求的用户ID
     */
    @NotNull(message = "请求用户ID不能为空")
    private Long requestUserId;

    /**
     * 接收方用户ID
     */
    @NotNull(message = "接收用户ID不能为空")
    private Long receiveUserId;

    /**
     * 处理状态 1:同意 2:拒绝
     */
    @NotNull(message = "处理状态不能为空")
    private Integer status;

    /**
     * 回复
     */
    private String comment;
}
