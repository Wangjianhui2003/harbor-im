package com.jianhui.project.harbor.server.netty.wbsocket;

import com.jianhui.project.harbor.server.netty.IMChannelHandler;
import com.jianhui.project.harbor.server.netty.IMServer;
import com.jianhui.project.harbor.server.netty.wbsocket.decode.MessageProtocolDecoder;
import com.jianhui.project.harbor.server.netty.wbsocket.encode.MessageProtocolEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * websocket服务器与web端做交互
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "websocket",value = "enable", havingValue = "true")
public class WebSocketServer implements IMServer {

    @Value("${websocket.port}")
    private int port;

    private volatile boolean ready = false;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;


    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        bootstrap.group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        //心跳检测
                        pipeline.addLast("idle-handler",new IdleStateHandler(60,0,0, TimeUnit.SECONDS));
                        //encoder decoder
                        pipeline.addLast("http-codec",new HttpServerCodec());
                        //聚合
                        pipeline.addLast("http-aggregator",new HttpObjectAggregator(65535));
                        //写大文件
                        pipeline.addLast("chunked-write",new ChunkedWriteHandler());
                        //处理websocket连接
                        pipeline.addLast("ws-handler",new WebSocketServerProtocolHandler("/im"));
                        //协议编解码器
                        pipeline.addLast("msg-encoder",new MessageProtocolEncoder());
                        pipeline.addLast("msg-decoder",new MessageProtocolDecoder());
                        //业务处理：登录、心跳
                        pipeline.addLast("im-handler",new IMChannelHandler());
                    }
                })
                // bootstrap 还可以设置TCP参数，根据需要可以分别设置主线程池和从线程池参数，来优化服务端性能。
                // 其中主线程池使用option方法来设置，从线程池使用childOption方法设置。
                // backlog表示主线程池中在套接口排队的最大数量，队列由未连接队列（三次握手未完成的）和已连接队列
                .option(ChannelOption.SO_BACKLOG,5)
                // 表示连接保活，相当于心跳机制，默认为7200s
                .childOption(ChannelOption.SO_KEEPALIVE,true);
        try {
            // 绑定端口，启动select线程，轮询监听channel事件，监听到事件之后就会交给从线程池处理
            bootstrap.bind(port).sync().channel();
            // 就绪标志
            this.ready = true;
            log.info("websocket server 初始化完成,端口：{}", port);
        } catch (InterruptedException e) {
            log.info("websocket server 初始化异常", e);
        }
    }

    @Override
    public void stop() {
        if(bossGroup != null && !bossGroup.isShuttingDown() && !bossGroup.isShutdown()){
            bossGroup.shutdownGracefully();
        }
        if(workerGroup != null && !workerGroup.isShuttingDown() && !workerGroup.isShutdown()){
            workerGroup.shutdownGracefully();
        }
        ready = false;
        log.info("websocket server 停止");
    }
}
