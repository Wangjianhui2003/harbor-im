package com.jianhui.project.harbor.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "webrtc")
public class WebRTCConfig {

    private String maxChannel;

    private List<ICEServer> iceServers;
}
