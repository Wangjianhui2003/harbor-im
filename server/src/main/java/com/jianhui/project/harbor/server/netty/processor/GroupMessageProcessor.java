package com.jianhui.project.harbor.server.netty.processor;

import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMRecvInfo;
import com.jianhui.project.harbor.server.service.GroupMessageDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GroupMessageProcessor extends AbstractMsgProcessor<IMRecvInfo> {

    private final GroupMessageDeliveryService groupMessageDeliveryService;

    @Override
    public IMCmdType getCmdType() {
        return IMCmdType.GROUP_MESSAGE;
    }

    @Override
    public void process(IMRecvInfo recvInfo) {
        groupMessageDeliveryService.deliverAndAwaitAck(recvInfo);
    }
}
