package com.jianhui.project.harbor.server.netty;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserChannelCxtMap {

    /**
     *  维护userid和channelhandlercontext的关联，格式:map<userid,map<terminal，ctx>>
     */
    private static ConcurrentHashMap<Long, Map<Integer, ChannelHandlerContext>> channelMap = new ConcurrentHashMap<>();

    public static void addChannelCxt(Long userId,Integer terminal,ChannelHandlerContext cxt){
        //一个terminal对应一个channel
        channelMap.computeIfAbsent(userId,k -> new ConcurrentHashMap<>()).put(terminal,cxt);
    }

    public static void  removeChannelCxt(Long userId,Integer terminal){
        if(userId != null && terminal != null && channelMap.containsKey(userId)){
            Map<Integer, ChannelHandlerContext> map = channelMap.get(userId);
            map.remove(terminal);
            if(map.isEmpty()){
                channelMap.remove(userId);
            }
        }
    }

    public static ChannelHandlerContext getChannelCxt(Long userId,Integer terminal){
        if(userId != null && terminal != null && channelMap.containsKey(userId)){
            Map<Integer, ChannelHandlerContext> map = channelMap.get(userId);
            if (map.containsKey(terminal)){
                return map.get(terminal);
            }
        }
        return null;
    }
}
