package com.jianhui.project.harbor.platform.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "harbor.mq.private-message")
public class PrivateMessageMQProperties {

    private Consumer persist = new Consumer();

    private Consumer dispatch = new Consumer();

    private Retry retry = new Retry();

    @Data
    public static class Consumer {

        private int consumeThreadMin = 32;

        private int consumeThreadMax = 64;

        private int consumeMessageBatchMaxSize = 32;

        private int pullBatchSize = 64;
    }

    @Data
    public static class Retry {

        private int maxAttempts = 3;

        private long retryDelayMs = 1000;

        private int sendTimeoutMs = 5000;
    }
}
