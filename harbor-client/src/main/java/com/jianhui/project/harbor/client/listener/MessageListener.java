package com.jianhui.project.harbor.client.listener;


import com.jianhui.project.harbor.common.model.IMSendResult;

import java.util.List;

public interface MessageListener<T> {

    void process(List<IMSendResult<T>> result);

}
