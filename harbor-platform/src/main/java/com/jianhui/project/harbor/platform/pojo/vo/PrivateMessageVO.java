package com.jianhui.project.harbor.platform.pojo.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jianhui.project.harbor.common.serializer.DateToLongSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "私聊消息VO")
public class PrivateMessageVO {

    @Schema(description = " 消息id")
    private Long id;

    @Schema(description = " 发送者id")
    private Long sendId;

    @Schema(description = " 接收者id")
    private Long recvId;

    @Schema(description = " 发送内容")
    private String content;

    @Schema(description = "消息内容类型 MessageType")
    private Integer type;

    @Schema(description = " 状态")
    private Integer status;

    @Schema(description = " 发送时间")
    @JsonSerialize(using = DateToLongSerializer.class)
    private Date sendTime;
}
