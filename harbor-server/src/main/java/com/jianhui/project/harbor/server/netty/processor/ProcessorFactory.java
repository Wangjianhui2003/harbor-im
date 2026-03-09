package com.jianhui.project.harbor.server.netty.processor;

import com.jianhui.project.harbor.common.enums.IMCmdType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 消息处理器工厂
 */
@Component
public class ProcessorFactory implements InitializingBean {

    private static final Map<IMCmdType, AbstractMsgProcessor<?>> PROCESSOR_MAP = new EnumMap<>(IMCmdType.class);

    private final List<AbstractMsgProcessor<?>> processors;

    public ProcessorFactory(List<AbstractMsgProcessor<?>> processors) {
        this.processors = processors;
    }

    @Override
    public void afterPropertiesSet() {
        PROCESSOR_MAP.clear();
        for (AbstractMsgProcessor<?> processor : processors) {
            IMCmdType cmdType = Objects.requireNonNull(processor.getCmdType(),
                    () -> "Processor cmd type can not be null: " + processor.getClass().getName());
            AbstractMsgProcessor<?> previous = PROCESSOR_MAP.putIfAbsent(cmdType, processor);
            if (previous != null) {
                throw new IllegalStateException("Duplicate processor for cmd " + cmdType
                        + ": " + previous.getClass().getName()
                        + " and " + processor.getClass().getName());
            }
        }
    }

    public static AbstractMsgProcessor getProcessor(IMCmdType cmd) {
        Objects.requireNonNull(cmd, "cmd can not be null");
        AbstractMsgProcessor<?> processor = PROCESSOR_MAP.get(cmd);
        if (processor == null) {
            throw new IllegalArgumentException("No processor found for cmd: " + cmd);
        }
        return processor;
    }
}
