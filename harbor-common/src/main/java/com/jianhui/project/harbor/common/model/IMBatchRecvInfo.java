package com.jianhui.project.harbor.common.model;

import lombok.Data;

import java.util.List;

@Data
public class IMBatchRecvInfo {

    /**
     * 按 server 分组后的一批待投递消息
     */
    private List<IMRecvInfo> messages;
}
