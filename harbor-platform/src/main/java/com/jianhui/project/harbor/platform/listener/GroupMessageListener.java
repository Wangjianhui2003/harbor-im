package com.jianhui.project.harbor.platform.listener;

import com.jianhui.project.harbor.client.annotation.IMListener;
import com.jianhui.project.harbor.client.listener.MessageListener;
import com.jianhui.project.harbor.common.enums.IMListenerType;
import com.jianhui.project.harbor.common.enums.IMSendCode;
import com.jianhui.project.harbor.common.model.IMSendResult;
import com.jianhui.project.harbor.platform.pojo.vo.GroupMessageVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 获取消息发送结果,进行日志打印和相应处理
 */
@Slf4j
@IMListener(type = IMListenerType.GROUP_MESSAGE)
@AllArgsConstructor
public class GroupMessageListener implements MessageListener<GroupMessageVO> {

    @Override
    public void process(List<IMSendResult<GroupMessageVO>> results) {
        for(IMSendResult<GroupMessageVO> result:results){
            GroupMessageVO messageInfo = result.getData();
            if (result.getCode().equals(IMSendCode.SUCCESS.code())) {
                log.info("消息送达，消息id:{}，发送者:{},接收者:{},终端:{}", messageInfo.getId(), result.getSender().getId(), result.getReceiver().getId(), result.getReceiver().getTerminal());
            }
        }
    }
}
