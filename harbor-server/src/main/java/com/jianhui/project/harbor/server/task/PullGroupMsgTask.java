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
@RedisMQListener(queue = IMRedisKey.IM_MESSAGE_GROUP_QUEUE,batchSize = 10)
public class PullGroupMsgTask extends AbstractPullMessageTask<IMRecvInfo> {

    @Override
    public void onMessage(IMRecvInfo data) {
        AbstractMsgProcessor processor = ProcessorFactory.getProcessor(IMCmdType.GROUP_MESSAGE);
        processor.process(data);
    }

}
