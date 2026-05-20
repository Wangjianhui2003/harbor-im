package com.jianhui.project.harbor.server.netty.processor;

import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMRecvInfo;
import com.jianhui.project.harbor.server.service.PrivateMessageDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrivateMessageProcessor extends AbstractMsgProcessor<IMRecvInfo> {

    private final PrivateMessageDeliveryService privateMessageDeliveryService;

    @Override
    public IMCmdType getCmdType() {
        return IMCmdType.PRIVATE_MESSAGE;
    }

    @Override
    public void process(IMRecvInfo recvInfo) {
        privateMessageDeliveryService.deliverAndAwaitAck(recvInfo);
    }
}
