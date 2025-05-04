package com.jianhui.project.harbor.platform.pojo.resp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaptchaResp {

    private String captchaPic;

    private String captchaKey;
}
