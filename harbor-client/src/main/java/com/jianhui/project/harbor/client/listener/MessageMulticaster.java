package com.jianhui.project.harbor.client.listener;

import com.alibaba.fastjson.JSONObject;
import com.jianhui.project.harbor.client.annotation.IMListener;
import com.jianhui.project.harbor.common.enums.IMListenerType;
import com.jianhui.project.harbor.common.model.IMSendResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * 消息广播类,主要是将SendResult中的data转为对应的消息类型（反射获取注解信息）
 * 发送给对应的listener,复用了消息转换的逻辑
 */
@Component
public class MessageMulticaster {

    @Autowired(required = false)
    private List<MessageListener> listeners = Collections.emptyList();

    public void multicast(IMListenerType listenerType, List<IMSendResult> results) {
        if(results == null || results.isEmpty()) {
            return;
        }
        for (MessageListener listener : listeners) {
            IMListener annotation = listeners.getClass().getAnnotation(IMListener.class);
            if(annotation != null && (annotation.type().equals(listenerType) || annotation.type().equals(IMListenerType.ALL))) {
                results.forEach(result -> {
                    //将消息数据转换成对应的类型
                    if(result.getData() instanceof JSONObject){
                        Type superClass = listener.getClass().getGenericInterfaces()[0];
                        Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
                        JSONObject data = (JSONObject)result.getData();
                        result.setData(data.toJavaObject(type));
                    }
                });
                listener.process(results);
            }
        }
    }
}
