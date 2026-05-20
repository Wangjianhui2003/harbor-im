package com.jianhui.project.harbor.server.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "harbor.mq.group-message")
public class GroupMessageMQProperties {

    private Consumer consumer = new Consumer();

    private Producer producer = new Producer();

    private Delivery delivery = new Delivery();

    private Dedup dedup = new Dedup();

    @Data
    public static class Consumer {

        private int consumeThreadMin = 32;

        private int consumeThreadMax = 64;

        private int consumeMessageBatchMaxSize = 32;

        private int pullBatchSize = 64;
    }

    @Data
    public static class Producer {

        private int sendTimeoutMs = 5000;
    }

    @Data
    public static class Delivery {

        private long ackTimeoutMs = 5000;
    }

    @Data
    public static class Dedup {

        private long sendExpireSeconds = 30;

        private long deliveryExpireSeconds = 300;
    }
}
