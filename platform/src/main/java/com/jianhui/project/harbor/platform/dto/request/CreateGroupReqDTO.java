package com.jianhui.project.harbor.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
@Schema(description = "创建群聊请求")
public class CreateGroupReqDTO {

    @NotBlank(message = "群名称不可为空")
    @Length(max = 20, message = "群名称长度不能大于20")
    @Schema(description = "群名称")
    private String name;

    @Length(max = 1024, message = "群公告长度不能大于1024")
    @Schema(description = "群公告")
    private String notice;

    @Min(value = 0, message = "加入类型不合法")
    @Max(value = 2, message = "加入类型不合法")
    @Schema(description = "加入类型 0:直接加入 1:需要管理员同意 2:禁止加入")
    private Integer joinType;

    @Size(max = 50, message = "一次最多只能邀请50位好友")
    @Schema(description = "创建后立即邀请的好友ID列表")
    private List<Long> friendIds;
}
