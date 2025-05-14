package com.jianhui.project.harbor.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.jianhui.project.harbor.platform.mapper")
@ComponentScan(basePackages = {"com.jianhui.project.harbor"})
@EnableCaching
public class HarborPlatformApp {
    public static void main(String[] args) {
        SpringApplication.run(HarborPlatformApp.class);
    }
}
