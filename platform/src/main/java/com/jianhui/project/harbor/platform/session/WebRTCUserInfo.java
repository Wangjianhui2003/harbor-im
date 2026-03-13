package com.jianhui.project.harbor.platform.session;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 用于WebRTC的UserInfo
 */
@Data
@Schema(description = "用户信息")
public class WebRTCUserInfo {

    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String headImage;

    @Schema(description = "是否开启摄像头")
    private Boolean isCameraOpen;

    @Schema(description = "是否开启麦克风")
    private Boolean isMicrophoneOpen;
}
