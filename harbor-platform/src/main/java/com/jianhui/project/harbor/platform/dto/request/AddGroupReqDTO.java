package com.jianhui.project.harbor.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加入群聊请求DTO
 */
@Data
public class AddGroupReqDTO {

    /**
     * 群组ID
     */
    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    /**
     * 请求留言
     */
    private String requestNote;
}
