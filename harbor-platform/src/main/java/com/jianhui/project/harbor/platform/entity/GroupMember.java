package com.jianhui.project.harbor.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 群成员
 * @TableName t_group_member
 */
@TableName(value ="t_group_member")
@Data
public class GroupMember {
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
     * 用户id
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userNickname;

    /**
     * 显示昵称备注
     */
    private String remarkNickname;

    /**
     * 角色
     */
    private Integer role;

    /**
     * 用户头像
     */
    private String headImage;

    /**
     * 显示群名备注
     */
    private String remarkGroupName;

    /**
     * 是否已退出
     */
    private Integer quit;

    /**
     * 退出时间
     */
    private Date quitTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建时间
     */
    private Date createdTime;
}