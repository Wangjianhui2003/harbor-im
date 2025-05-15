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

import java.util.List;

/**
 * 群聊消息处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupMessageProcessor extends AbstractMsgProcessor<IMRecvInfo> {

    private final RedisMQTemplate redisMQTemplate;

    @Override
    public void process(IMRecvInfo recvInfo) {
        IMUserInfo sender = recvInfo.getSender();
        List<IMUserInfo> receivers = recvInfo.getReceivers();
        log.info("接收到群消息，发送者:{},接收用户数量:{}，内容:{}", sender.getId(), receivers.size(), recvInfo.getData());
        for (IMUserInfo receiver : receivers) {
            try {
                ChannelHandlerContext ChannelCtx = UserChannelCxtMap.getChannelCtx(receiver.getId(), receiver.getTerminal());
                if (ChannelCtx != null) {
                    IMSendInfo<Object> sendInfo = new IMSendInfo<>();
                    sendInfo.setCmd(IMCmdType.PRIVATE_MESSAGE.code());
                    sendInfo.setData(recvInfo.getData());
                    ChannelCtx.writeAndFlush(sendInfo);
                    sendResult(recvInfo, IMSendCode.SUCCESS);
                } else {
                    log.error("未找到接收者channel,接收者:{},内容:{}",receiver.getId(),recvInfo.getData());
                    sendResult(recvInfo,IMSendCode.NOT_FIND_CHANNEL);
                }
            } catch (Exception e) {
                sendResult(recvInfo,IMSendCode.UNKONW_ERROR);
                log.error("发送异常，,接收者:{}，内容:{}",receiver.getId(), recvInfo.getData(), e);
            }
        }
    }

    private void sendResult(IMRecvInfo recvInfo, IMSendCode sendCode) {
        IMSendResult<Object> sendResult = new IMSendResult<>();
        sendResult.setSender(recvInfo.getSender());
        sendResult.setReceiver(recvInfo.getReceivers().get(0));
        sendResult.setData(recvInfo.getData());
        sendResult.setCode(sendCode.code());
        String key = String.join(":", IMRedisKey.IM_RESULT_GROUP_QUEUE, recvInfo.getServiceName());
        redisMQTemplate.opsForList().rightPush(key,sendResult);
    }
}
