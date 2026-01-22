package com.jianhui.project.harbor.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "图片上传VO")
public class UploadImageRespDTO {

    @Schema(description = "原图")
    private String originUrl;

    @Schema(description = "缩略图")
    private String thumbUrl;
}
