package com.jianhui.project.harbor.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.jianhui.project.harbor")
public class HarborServerApp {
    static void main(String[] args) {
        SpringApplication.run(HarborServerApp.class, args);

        System.out.println("hello");
    }
}
