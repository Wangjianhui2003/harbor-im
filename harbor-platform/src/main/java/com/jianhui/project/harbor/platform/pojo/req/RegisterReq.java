package com.jianhui.project.harbor.platform.pojo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户注册DTO")
public class RegisterReq {

    private String email;

    private String username;

    private String password;

    private String nickname;

    private String captcha;

    private String captchaKey;
}
