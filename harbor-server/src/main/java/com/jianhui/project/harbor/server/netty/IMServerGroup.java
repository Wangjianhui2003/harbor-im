package com.jianhui.project.harbor.server.netty;

import com.jianhui.project.harbor.common.constant.IMRedisKey;
import com.jianhui.project.harbor.common.mq.RedisMQTemplate;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class IMServerGroup implements CommandLineRunner {

    public static volatile long serverId = 0;

    private final RedisMQTemplate redisMQTemplate;

    private final List<IMServer> imServers;

    public boolean isReady() {
        for (IMServer imServer : imServers) {
            if (!imServer.isReady()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run(String... args) throws Exception {
        String key = IMRedisKey.IM_MAX_SERVER_ID;
        //为当前ServerGroup的id赋值
        serverId = redisMQTemplate.opsForValue().increment(key,1);
        for (IMServer imServer : imServers) {
            imServer.start();
        }
    }

    @PreDestroy
    public void destory(){
        log.info("IMServerGroup:{} destroy",serverId);
        for (IMServer imServer : imServers) {
            imServer.stop();
        }
    }
}
