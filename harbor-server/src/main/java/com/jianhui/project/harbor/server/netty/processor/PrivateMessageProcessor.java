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

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMessageProcessor extends AbstractMsgProcessor<IMRecvInfo> {

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void process(IMRecvInfo recvInfo) {
        IMUserInfo sender = recvInfo.getSender();
        IMUserInfo receiver = recvInfo.getReceivers().get(0);
        log.info("接收到私聊消息，发送者:{},客户端:{},接收者:{}，客户端:{},内容:{}", sender.getId(),sender.getTerminal(), receiver.getId(),receiver.getTerminal(), recvInfo.getData());
        try {
            ChannelHandlerContext channelCtx = UserChannelCxtMap.getChannelCtx(receiver.getId(), receiver.getTerminal());
            if (channelCtx != null) {
                // 推送消息到用户
                IMSendInfo<Object> sendInfo = new IMSendInfo<>();
                sendInfo.setCmd(IMCmdType.PRIVATE_MESSAGE.code());
                sendInfo.setData(recvInfo.getData());
                channelCtx.channel().writeAndFlush(sendInfo);
                // 消息发送成功确认
                sendResult(recvInfo, IMSendCode.SUCCESS);
                log.info("ws发送成功，发送者:{},接收者:{}，内容:{}", sender.getId(), receiver.getId(), recvInfo.getData());
            } else {
                // 消息推送失败确认
                sendResult(recvInfo, IMSendCode.NOT_FIND_CHANNEL);
                log.error("未找到channel，发送者:{},接收者:{}，内容:{}", sender.getId(), receiver.getId(), recvInfo.getData());
            }
        } catch (Exception e) {
            // 消息推送失败确认
            sendResult(recvInfo, IMSendCode.UNKONW_ERROR);
            log.error("发送异常，发送者:{},接收者:{}，内容:{}", sender.getId(), receiver.getId(), recvInfo.getData(), e);
        }

    }

    private void sendResult(IMRecvInfo recvInfo, IMSendCode sendCode) {
        if (recvInfo.getIsSendBack()) {
            IMSendResult<Object> result = new IMSendResult<>();
            result.setSender(recvInfo.getSender());
            result.setReceiver(recvInfo.getReceivers().get(0));
            result.setCode(sendCode.code());
            result.setData(recvInfo.getData());
            // 推送到结果队列
            asyncSendToMQ(IMMQConstant.PRIVATE_RESULT_TOPIC_PREFIX + recvInfo.getServiceName(),result);
        }
    }

    private void asyncSendToMQ(String topic,IMSendResult<Object> imSendResult) {
        rocketMQTemplate.asyncSend(topic,imSendResult, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("私聊消息发送结果回推成功:msg:{}",imSendResult.getData());
            }

            @Override
            public void onException(Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        },5000);
    }
}
