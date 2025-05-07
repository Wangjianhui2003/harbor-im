package com.jianhui.project.harbor.server.netty.processor;

import cn.hutool.core.bean.BeanUtil;
import com.jianhui.project.harbor.common.constant.IMConstant;
import com.jianhui.project.harbor.common.constant.IMRedisKey;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMHeartbeatInfo;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.common.mq.RedisMQTemplate;
import com.jianhui.project.harbor.server.constant.ChannelAttrKey;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class HeartbeatProcessor extends AbstractMsgProcessor<IMHeartbeatInfo> {

    private final RedisMQTemplate redisMQTemplate;

    @Override
    public void process(ChannelHandlerContext ctx, IMHeartbeatInfo data) {
        //发送心跳
        IMSendInfo<Object> sendInfo = new IMSendInfo<>();
        sendInfo.setCmd(IMCmdType.HEART_BEAT.code());
        ctx.channel().writeAndFlush(sendInfo);
        //心跳次数+1
        AttributeKey<Long> heartBeatTimes = AttributeKey.valueOf(ChannelAttrKey.HEARTBEAT_TIMES);
        Long l = ctx.channel().attr(heartBeatTimes).get();
        ctx.channel().attr(heartBeatTimes).set(++l);
        // 每10次心跳延迟redis过期时间
        if(l % 10 == 0){
            AttributeKey<Long> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
            Long userId = ctx.channel().attr(userIdAttr).get();
            AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
            Integer terminal = ctx.channel().attr(terminalAttr).get();

            String key = String.join(":",IMRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
            redisMQTemplate.expire(key,IMConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
        }
        // 记录心跳
        AttributeKey<Long> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
        Long userId = ctx.channel().attr(userIdAttr).get();
        log.debug("心跳,userId:{},channel id:{}",userId,ctx.channel().id().asLongText());
    }

    @Override
    public IMHeartbeatInfo transForm(Object o) {
        HashMap map = (HashMap) o;
        return BeanUtil.fillBeanWithMap(map, new IMHeartbeatInfo(),false);
    }
}
