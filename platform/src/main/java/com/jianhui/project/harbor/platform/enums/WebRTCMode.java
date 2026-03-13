package com.jianhui.project.harbor.platform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 实时通话类型
 */

@Getter
@AllArgsConstructor
public enum WebRTCMode {

    /**
     * 视频通话
     */
    VIDEO("VIDEO"),

    /**
     * 语音通话
     */
    VOICE("VOICE");

    private final String value;

}
