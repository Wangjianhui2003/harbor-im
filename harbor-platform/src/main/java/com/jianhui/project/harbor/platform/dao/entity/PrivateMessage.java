package com.jianhui.project.harbor.platform.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 私聊消息
 *
 * @TableName t_private_message
 */
@TableName(value = "t_private_message")
@Data
public class PrivateMessage {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送用户id
     */
    private Long sendId;

    /**
     * 接收用户id
     */
    private Long recvId;

    /**
     * 发送内容
     */
    private String content;

    /**
     * 消息类型 0:文字 1:图片 2:文件 3:语音 4:视频 21:提示
     */
    private Integer type;

    /**
     * 状态 1:已发送 2:已读 3:撤回
     */
    private Integer status;

    /**
     * 发送时间
     */
    private Date sendTime;
}