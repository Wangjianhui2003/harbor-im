package com.jianhui.project.harbor.server.netty.processor;

import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.server.util.SpringContextHolder;

public class ProcessorFactory {

    public static AbstractMsgProcessor getProcessor(IMCmdType cmd){
        return switch(cmd){
            case LOGIN -> SpringContextHolder.getBean("loginProcessor");
            case HEART_BEAT -> SpringContextHolder.getBean("heartbeatProcessor");
            default -> null;
        };
    }
}
