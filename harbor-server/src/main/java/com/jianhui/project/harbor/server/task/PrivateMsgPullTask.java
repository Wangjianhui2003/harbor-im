package com.jianhui.project.harbor.server.task;

import com.jianhui.project.harbor.common.constant.IMRedisKey;
import com.jianhui.project.harbor.common.enums.IMCmdType;
import com.jianhui.project.harbor.common.model.IMRecvInfo;
import com.jianhui.project.harbor.common.mq.RedisMQListener;
import com.jianhui.project.harbor.server.netty.processor.AbstractMsgProcessor;
import com.jianhui.project.harbor.server.netty.processor.ProcessorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RedisMQListener(queue = IMRedisKey.IM_MESSAGE_PRIVATE_QUEUE,batchSize = 10)
public class PrivateMsgPullTask extends AbstractMsgPullTask<IMRecvInfo> {

    @Override
    public void onMessage(IMRecvInfo data) {
        AbstractMsgProcessor processor = ProcessorFactory.getProcessor(IMCmdType.PRIVATE_MESSAGE);
        processor.process(data);
    }
}
