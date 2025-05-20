package com.jianhui.project.harbor.client.task;

import com.jianhui.project.harbor.client.listener.MessageMulticaster;
import com.jianhui.project.harbor.common.constant.IMRedisKey;
import com.jianhui.project.harbor.common.enums.IMListenerType;
import com.jianhui.project.harbor.common.model.IMSendResult;
import com.jianhui.project.harbor.common.mq.RedisMQListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@RedisMQListener(queue = IMRedisKey.IM_RESULT_SYSTEM_QUEUE, batchSize = 100)
public class SystemMsgResultPullTask extends AbstractMsgResultPullTask<IMSendResult> {

    private final MessageMulticaster multicaster;

    @Override
    public void onMessage(List<IMSendResult> results) {
        multicaster.multicast(IMListenerType.SYSTEM_MESSAGE, results);
    }

}
