package com.jianhui.project.harbor.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "视频上传VO")
public class UploadVideoRespDTO {

    @Schema(description = "视频url")
    private String url;

    @Schema(description = "封面url")
    private String coverUrl;

    @Schema(description = "时长(毫秒)")
    private Long duration;
}
