package com.jianhui.project.harbor.server.netty.processor;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.jianhui.project.harbor.common.constant.IMConstant;
import com.jianhui.project.harbor.common.constant.IMRedisKey;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMLoginInfo;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.common.model.IMSessionInfo;
import com.jianhui.project.harbor.common.mq.RedisMQTemplate;
import com.jianhui.project.harbor.common.util.JwtUtil;
import com.jianhui.project.harbor.server.constant.ChannelAttrKey;
import com.jianhui.project.harbor.server.netty.IMServerGroup;
import com.jianhui.project.harbor.server.netty.UserChannelCxtMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * websocket登录信息处理器
 * 建立ws连接后要校验用户信息
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class LoginProcessor extends AbstractMsgProcessor<IMLoginInfo>{

    private final RedisMQTemplate redisMQTemplate;
    @Value("${jwt.accessToken.secret}")
    private String accessTokenSecret;

    @Override
    public void process(ChannelHandlerContext ctx, IMLoginInfo loginInfo) {
        //校验jwt
        if(!JwtUtil.checkSign(loginInfo.getAccessToken(),accessTokenSecret)){
            ctx.channel().close();
            log.warn("用户token校验不通过，强制下线,token:{}",loginInfo.getAccessToken());
            return;
        }
        //获取用户信息
        String info = JwtUtil.getInfo(loginInfo.getAccessToken());
        IMSessionInfo sessionInfo = JSON.parseObject(info, IMSessionInfo.class);
        Long userId = sessionInfo.getUserId();
        Integer terminal = sessionInfo.getTerminal();
        log.info("用户websocket登录成功，userId:{},ServerId:{}",userId,IMServerGroup.serverId);

        ChannelHandlerContext context = UserChannelCxtMap.getChannelCtx(userId, terminal);
        //已经有相同userid + terminal的channel存在，强制下线之前登录的用户
        if(context != null && ctx.channel().id().equals(context.channel().id())){
            IMSendInfo<Object> sendInfo = new IMSendInfo<>();
            sendInfo.setCmd(IMCmdType.FORCE_LOGUT.code());
            //发送信息到前端,前端强制下线
            sendInfo.setData("您已在其他地方登录，将被强制下线");
            context.channel().writeAndFlush(sendInfo);
            log.info("异地登录，强制下线,userId:{},terminal:{}",userId,terminal);
        }

        //保存当前登录的channel context
        UserChannelCxtMap.addChannelCxt(userId,terminal,ctx);
        //保存用户id到channel
        AttributeKey<Long> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
        ctx.channel().attr(userIdAttr).set(userId);
        //保存终端类型到channel
        AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
        ctx.channel().attr(terminalAttr).set(terminal);
        //保存心跳次数到channel
        AttributeKey<Long> heartBeatAttr = AttributeKey.valueOf(ChannelAttrKey.HEARTBEAT_TIMES);
        ctx.channel().attr(heartBeatAttr).set(0L);

        //保存用户对应server_id到redis
        String key = String.join(":", IMRedisKey.IM_USER_SERVER_ID,userId.toString(), terminal.toString());
        redisMQTemplate.opsForValue().set(key, IMServerGroup.serverId, IMConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);

        //ws传送信息
        IMSendInfo<Object> sendInfo = new IMSendInfo<>();
        sendInfo.setCmd(IMCmdType.LOGIN.code());
        ctx.channel().writeAndFlush(sendInfo);
    }

    @Override
    public IMLoginInfo transForm(Object o) {
        HashMap map = (HashMap)o;
        return BeanUtil.fillBeanWithMap(map, new IMLoginInfo(),false);
    }
}
