package com.jianhui.project.harbor.server.netty;

import com.jianhui.project.harbor.common.constant.IMRedisKey;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.common.mq.RedisMQTemplate;
import com.jianhui.project.harbor.server.constant.ChannelAttrKey;
import com.jianhui.project.harbor.server.netty.processor.AbstractMsgProcessor;
import com.jianhui.project.harbor.server.netty.processor.ProcessorFactory;
import com.jianhui.project.harbor.server.util.SpringContextHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 长连接下 文本帧的处理器
 * 实现浏览器发送文本回写
 * 浏览器连接状态监控
 */
@Slf4j
public class IMChannelHandler extends SimpleChannelInboundHandler<IMSendInfo> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMSendInfo sendInfo) throws Exception {
        // 使用对应处理器进行处理
        AbstractMsgProcessor processor = ProcessorFactory.getProcessor(IMCmdType.fromCode(sendInfo.getCmd()));
        processor.process(ctx, processor.transForm(sendInfo));
    }

    /**
     * 打印异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("异常信息:{}", cause.getMessage());
    }

    /**
     * 监控浏览器上线
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        log.info("连接成功:{}", ctx.channel().id().asLongText());
    }

    /**
     * channel移除后
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        AttributeKey<Long> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
        Long userId = ctx.channel().attr(userIdAttr).get();
        AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
        Integer terminal = ctx.channel().attr(terminalAttr).get();
        ChannelHandlerContext context = UserChannelCxtMap.getChannelCtx(userId, terminal);
        // 如果当前上下文和之前的上下文相同，则移除，避免异地登录误删
        if (context != null && ctx.channel().id().equals(context.channel().id())) {
            // 移除上下文
            UserChannelCxtMap.removeChannelCxt(userId, terminal);
            RedisMQTemplate redisMQTemplate = SpringContextHolder.getBean("redisMQTemplate");
            //清除redis中用户的server id
            String key = String.join(":", IMRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
            redisMQTemplate.delete(key);
            log.info("断开连接,userId:{},终端类型:{},channel id:{}", userId, terminal, ctx.channel().id().asLongText());
        }
    }

    /**
     * 监控心跳
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleState state = ((IdleStateEvent) evt).state();
            //是未读闲置事件
            if(state == IdleState.READER_IDLE){
                // 在规定时间内没有收到客户端的上行数据, 主动断开连接
                AttributeKey<Long> attr = AttributeKey.valueOf("USER_ID");
                Long userId = ctx.channel().attr(attr).get();
                AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
                Integer terminal = ctx.channel().attr(terminalAttr).get();
                log.info("心跳超时，即将断开连接,用户id:{},终端类型:{} ", userId, terminal);
                ctx.channel().close();
            }
        }else {
            super.userEventTriggered(ctx,evt);
        }
    }
}
