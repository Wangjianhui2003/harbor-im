package com.jianhui.project.harbor.server.netty.processor;

import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.enums.IMSendCode;
import com.jianhui.project.harbor.common.model.IMRecvInfo;
import com.jianhui.project.harbor.common.model.IMSendInfo;
import com.jianhui.project.harbor.common.model.IMSendResult;
import com.jianhui.project.harbor.common.model.IMUserInfo;
import com.jianhui.project.harbor.server.netty.UserChannelCxtMap;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 群聊消息处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupMessageProcessor extends AbstractMsgProcessor<IMRecvInfo> {

    private final RocketMQTemplate rocketMQTemplate;

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
                    sendInfo.setCmd(IMCmdType.GROUP_MESSAGE.code());
                    sendInfo.setData(recvInfo.getData());
                    ChannelCtx.writeAndFlush(sendInfo);
                    sendResult(recvInfo, IMSendCode.SUCCESS);
                } else {
                    log.error("未找到接收者channel,接收者:{},内容:{}", receiver.getId(), recvInfo.getData());
                    sendResult(recvInfo, IMSendCode.NOT_FIND_CHANNEL);
                }
            } catch (Exception e) {
                sendResult(recvInfo, IMSendCode.UNKONW_ERROR);
                log.error("发送异常，,接收者:{}，内容:{}", receiver.getId(), recvInfo.getData(), e);
            }
        }
    }

    private void sendResult(IMRecvInfo recvInfo, IMSendCode sendCode) {
        IMSendResult<Object> sendResult = new IMSendResult<>();
        sendResult.setSender(recvInfo.getSender());
        sendResult.setReceiver(recvInfo.getReceivers().get(0));
        sendResult.setData(recvInfo.getData());
        sendResult.setCode(sendCode.code());
        asyncSendToMQ(IMMQConstant.GROUP_RESULT_TOPIC_PREFIX + recvInfo.getServiceName(), sendResult);
    }

    private void asyncSendToMQ(String topic, IMSendResult<Object> imSendResult) {
        rocketMQTemplate.asyncSend(topic, imSendResult, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("消息发送结果回推成功:msg,{}", imSendResult.getData());
            }

            @Override
            public void onException(Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }, 5000);
    }
}
