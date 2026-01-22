package com.jianhui.project.harbor.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OnlineTerminalRespDTO {

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "在线终端类型")
    private List<Integer> terminals;

}
