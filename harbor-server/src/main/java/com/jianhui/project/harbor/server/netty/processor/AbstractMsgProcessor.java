package com.jianhui.project.harbor.server.netty.processor;

import io.netty.channel.ChannelHandlerContext;

public abstract class AbstractMsgProcessor<T> {

    /**
     * 处理登录、心跳等和客户端沟通的信息需要实现
     */
    public void process(ChannelHandlerContext ctx, T data) {
    }

    /**
     * 处理群聊，私聊，系统消息需要实现
     */
    public void process(T data) {
    }

    /**
     * 将接受到的消息转换
     */
    public T transForm(Object o) {
        return (T) o;
    }

}
