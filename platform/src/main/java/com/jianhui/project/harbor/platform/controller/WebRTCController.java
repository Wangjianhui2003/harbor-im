package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.config.WebRTCConfig;
import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webrtc")
@RequiredArgsConstructor
public class WebRTCController {

    private final WebRTCConfig webRTCConfig;

    @GetMapping("/config")
    public Result<WebRTCConfig> loadWebRTCConfig() {
        return Results.success(webRTCConfig);
    }

}
