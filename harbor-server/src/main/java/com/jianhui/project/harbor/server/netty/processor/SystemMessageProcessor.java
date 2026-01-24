package com.jianhui.project.harbor.server.netty.processor;

import com.jianhui.project.harbor.common.constant.IMRedisKey;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.enums.IMSendCode;
import com.jianhui.project.harbor.common.model.IMRecvInfo;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.common.model.IMSendResult;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.common.mq.RedisMQTemplate;
import com.jianhui.project.harbor.server.netty.UserChannelCxtMap;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemMessageProcessor extends AbstractMsgProcessor<IMRecvInfo> {

    private final RedisMQTemplate redisMQTemplate;

    @Override
    public void process(IMRecvInfo recvInfo) {
        log.info("接收到系统消息,接收用户数量:{}，内容:{}", recvInfo.getReceivers().size(), recvInfo.getData());
        for (IMUserInfo userInfo : recvInfo.getReceivers()) {
            try {
                ChannelHandlerContext channelCtx = UserChannelCxtMap.getChannelCtx(userInfo.getId(), userInfo.getTerminal());
                if (channelCtx != null) {
                    IMSendInfo<Object> sendInfo = new IMSendInfo<>();
                    sendInfo.setCmd(IMCmdType.SYSTEM_MESSAGE.code());
                    sendInfo.setData(recvInfo.getData());
                    channelCtx.writeAndFlush(sendInfo);
                    //消息推送成功
                    sendResult(recvInfo, IMSendCode.SUCCESS);
                } else {
                    log.error("未找到接收者channel,接收者:{},内容:{}", userInfo.getId(), recvInfo.getData());
                    sendResult(recvInfo, IMSendCode.NOT_FIND_CHANNEL);
                }
            } catch (Exception e) {
                sendResult(recvInfo, IMSendCode.UNKONW_ERROR);
                log.error("发送异常，,接收者:{}，内容:{}", userInfo.getId(), recvInfo.getData(), e);
            }
        }
    }

    /**
     * 发送消息发送结果到redis队列
     */
    private void sendResult(IMRecvInfo recvInfo, IMSendCode sendCode) {
        //是否需要回发结果
        if (recvInfo.getIsSendBack()) {
            IMSendResult<Object> sendResult = new IMSendResult<>();
            sendResult.setReceiver(recvInfo.getReceivers().get(0));
            sendResult.setCode(sendCode.code());
            sendResult.setData(recvInfo.getData());
            // 推送到结果队列
            String key = String.join(":", IMRedisKey.IM_RESULT_SYSTEM_QUEUE, recvInfo.getServiceName());
            redisMQTemplate.opsForList().rightPush(key, sendResult);
        }
    }
}
