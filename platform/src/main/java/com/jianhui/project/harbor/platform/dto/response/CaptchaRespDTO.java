package com.jianhui.project.harbor.platform.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaptchaRespDTO {

    private String captchaPic;

    private String captchaKey;
}
