package com.jianhui.project.harbor.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "群成员信息VO")
public class GroupMemberRespDTO {

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "群内显示名称")
    private String showNickname;

    @Schema(description = "群内昵称备注")
    private String remarkNickname;

    @Schema(description = "头像")
    private String headImage;

    @Schema(description = "是否已退出")
    private Boolean quit;

    @Schema(description = "是否在线")
    private Boolean online;

    @Schema(description = "群名显示名称")
    private String showGroupName;

    @Schema(description = "群名备注")
    private String remarkGroupName;
}
