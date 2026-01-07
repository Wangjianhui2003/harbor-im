package com.jianhui.project.harbor.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户
 * @TableName t_user
 */
@TableName(value ="t_user")
@Data
public class User {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String headImage;

    /**
     * 用户头像缩略图
     */
    private String headImageThumb;

    /**
     * 密码
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 性别 0:男 1:女
     */
    private Integer sex;

    /**
     * 是否被封禁 0:否 1:是
     */
    private Integer isBanned;

    /**
     * 被封禁原因
     */
    private String reason;

    /**
     * 用户类型 1:普通用户 2:审核账户
     */
    private Integer type;

    /**
     * 0:直接加好友 1:同意后加好友
     */
    private Integer addType;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 最后登录时间
     */
    private Date lastLoginTime;

    /**
     * 地区
     */
    private String region;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建时间
     */
    private Date createdTime;
}