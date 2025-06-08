package com.jianhui.project.harbor.client;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.jianhui.project.harbor.client", "com.jianhui.project.harbor.common"})
public class IMAutoConfiguration {
}
