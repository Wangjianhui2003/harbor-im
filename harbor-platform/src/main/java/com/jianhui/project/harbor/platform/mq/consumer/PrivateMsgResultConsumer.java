package com.jianhui.project.harbor.platform.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jianhui.project.harbor.common.constant.IMMQConstant;
import com.jianhui.project.harbor.common.enums.IMSendCode;
import com.jianhui.project.harbor.common.model.IMSendResult;
import com.jianhui.project.harbor.platform.dao.entity.PrivateMessage;
import com.jianhui.project.harbor.platform.dto.response.PrivateMessageRespDTO;
import com.jianhui.project.harbor.platform.enums.MessageStatus;
import com.jianhui.project.harbor.platform.service.PrivateMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMsgResultConsumer implements ApplicationRunner {

    @Value("${rocketmq.name-server}")
    private String nameServerAddr;

    @Value("${spring.application.name}")
    private String appName;

    private final PrivateMessageService privateMessageService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DefaultMQPushConsumer consumer =
                new DefaultMQPushConsumer(IMMQConstant.PRIVATE_RESULT_MSG_CONSUMER_PREFIX + appName);

        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(IMMQConstant.PRIVATE_RESULT_TOPIC_PREFIX + appName, "*");

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                Set<Long> messageIds = new HashSet<>();
                for (MessageExt msg : list) {
                    byte[] body = msg.getBody();
                    String string = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(body)).toString();
                    IMSendResult imSendResult = JSON.parseObject(string, IMSendResult.class);
                    JSONObject jsonObject = (JSONObject)imSendResult.getData();
                    PrivateMessageRespDTO msgVO = jsonObject.toJavaObject(PrivateMessageRespDTO.class);
                    if (imSendResult.getCode().equals(IMSendCode.SUCCESS.code()) && msgVO.getId() != null){
                        messageIds.add(msgVO.getId());
                        log.info("消息送达，消息id:{}，发送者:{},接收者:{},终端:{}",
                                msgVO.getId(),
                                imSendResult.getSender().getId(),
                                imSendResult.getReceiver().getId(),
                                imSendResult.getReceiver().getTerminal());
                    }
                }
                //
                if(CollUtil.isNotEmpty(messageIds)){
                    UpdateWrapper<PrivateMessage> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.lambda().in(PrivateMessage::getId, messageIds)
                            .eq(PrivateMessage::getStatus, MessageStatus.UNSENT.code())
                            .set(PrivateMessage::getStatus, MessageStatus.SENT.code());
                    privateMessageService.update(updateWrapper);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
    }
}

