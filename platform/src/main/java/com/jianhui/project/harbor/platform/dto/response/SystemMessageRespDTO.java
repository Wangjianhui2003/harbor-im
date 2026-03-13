package com.jianhui.project.harbor.platform.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jianhui.project.harbor.common.serializer.DateToLongSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "系统消息VO")
public class SystemMessageRespDTO {

    @Schema(description = " 消息id")
    private Long id;

    @Schema(description = " 发送内容")
    private String content;

    @Schema(description = "消息内容类型 MessageType")
    private Integer type;

    @Schema(description = " 发送时间")
    @JsonSerialize(using = DateToLongSerializer.class)
    private Date sendTime;
}
