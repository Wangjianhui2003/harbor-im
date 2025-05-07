package com.jianhui.project.harbor.server.netty;

public interface IMServer {

    boolean isReady();

    void start();

    void stop();
}
