package com.jianhui.project.harbor.server.netty.processor;

import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.server.util.SpringContextHolder;

/**
 * 消息处理器工厂
 */
public class ProcessorFactory {

    public static AbstractMsgProcessor getProcessor(IMCmdType cmd){
        return switch(cmd){
            case LOGIN -> SpringContextHolder.getBean("loginProcessor");
            case HEARTBEAT -> SpringContextHolder.getBean("heartbeatProcessor");
            case PRIVATE_MESSAGE -> SpringContextHolder.getBean("privateMessageProcessor");
            case GROUP_MESSAGE -> SpringContextHolder.getBean("groupMessageProcessor");
            case SYSTEM_MESSAGE -> SpringContextHolder.getBean("systemMessageProcessor");
            default -> null;
        };
    }
}
