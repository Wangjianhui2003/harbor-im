package com.jianhui.project.harbor.server.event;

import org.springframework.context.ApplicationEvent;
/**
 * 服务器启动事件
 */

public class IMServerReadyEvent extends ApplicationEvent {
    public IMServerReadyEvent(Object source) {
        super(source);
    }
}
