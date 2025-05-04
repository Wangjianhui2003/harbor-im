package com.jianhui.project.harbor.platform.pojo.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "用户登录Resp")
public class LoginResp {

    @Schema(description = "每次请求都必须在header中携带accessToken")
    private String accessToken;

    @Schema(description = "accessToken过期时间(秒)")
    private Integer accessTokenExpiresIn;

    @Schema(description = "accessToken过期后，通过refreshToken换取新的token")
    private String refreshToken;

    @Schema(description = "refreshToken过期时间(秒)")
    private Integer refreshTokenExpiresIn;
}

