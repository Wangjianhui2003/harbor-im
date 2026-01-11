package com.jianhui.project.harbor.platform.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "视频上传VO")
public class UploadVideoVO {

    @Schema(description = "视频URL")
    private String url;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "视频时长(秒)")
    private Double duration;
}
