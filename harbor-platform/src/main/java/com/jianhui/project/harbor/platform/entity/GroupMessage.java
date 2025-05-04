package com.jianhui.project.harbor.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 群消息
 * @TableName t_group_message
 */
@TableName(value ="t_group_message")
@Data
public class GroupMessage {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群id
     */
    private Long groupId;

    /**
     * 发送用户id
     */
    private Long sendId;

    /**
     * 发送用户昵称
     */
    private String sendNickname;

    /**
     * 接收用户id,逗号分隔，为空表示发给所有成员
     */
    private String recvIds;

    /**
     * 发送内容
     */
    private String content;

    /**
     * 被@的用户id列表，逗号分隔
     */
    private String atUserIds;

    /**
     * 是否回执消息
     */
    private Integer receipt;

    /**
     * 回执消息是否完成
     */
    private Integer receiptOk;

    /**
     * 消息类型 0:文字 1:图片 2:文件 3:语音 4:视频 21:提示
     */
    private Integer type;

    /**
     * 状态 0:未发出  2:撤回 
     */
    private Integer status;

    /**
     * 发送时间
     */
    private Date sendTime;
}