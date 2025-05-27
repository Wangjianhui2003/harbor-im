package com.jianhui.project.harbor.platform.listener;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jianhui.project.harbor.client.annotation.IMListener;
import com.jianhui.project.harbor.client.listener.MessageListener;
import com.jianhui.project.harbor.common.enums.IMListenerType;
import com.jianhui.project.harbor.common.enums.IMSendCode;
import com.jianhui.project.harbor.common.model.IMSendResult;
import com.jianhui.project.harbor.platform.entity.PrivateMessage;
import com.jianhui.project.harbor.platform.enums.MessageStatus;
import com.jianhui.project.harbor.platform.pojo.vo.PrivateMessageVO;
import com.jianhui.project.harbor.platform.service.PrivateMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@IMListener(type = IMListenerType.PRIVATE_MESSAGE)
public class PrivateMessageListener implements MessageListener<PrivateMessageVO> {

    @Lazy
    @Autowired
    private PrivateMessageService privateMessageService;

    @Override
    public void process(List<IMSendResult<PrivateMessageVO>> results) {
        Set<Long> messageIds = new HashSet<>();
        for(IMSendResult<PrivateMessageVO> result : results){
            PrivateMessageVO messageInfo = result.getData();
            // 更新消息状态,这里只处理成功消息，失败的消息继续保持未读状态
            if (result.getCode().equals(IMSendCode.SUCCESS.code()) && !Objects.isNull(messageInfo.getId())) {
                messageIds.add(messageInfo.getId());
                log.info("消息送达，消息id:{}，发送者:{},接收者:{},终端:{}", messageInfo.getId(), result.getSender().getId(), result.getReceiver().getId(), result.getReceiver().getTerminal());
            }
        }
        // 批量修改状态
        if(CollUtil.isNotEmpty(messageIds)){
            UpdateWrapper<PrivateMessage> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda().in(PrivateMessage::getId, messageIds)
                .eq(PrivateMessage::getStatus, MessageStatus.UNSEND.code())
                .set(PrivateMessage::getStatus, MessageStatus.SENDED.code());
            privateMessageService.update(updateWrapper);
        }
    }
}
