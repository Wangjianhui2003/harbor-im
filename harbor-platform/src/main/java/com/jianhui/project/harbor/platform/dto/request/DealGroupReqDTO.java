package com.jianhui.project.harbor.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 处理群聊请求DTO
 */
@Data
public class DealGroupReqDTO {

    /**
     * 请求ID
     */
    @NotNull(message = "请求ID不能为空")
    private Long id;

    /**
     * 群组ID
     */
    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    /**
     * 发起请求的用户ID
     */
    @NotNull(message = "请求用户ID不能为空")
    private Long requestUserId;

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
