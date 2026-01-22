package com.jianhui.project.harbor.platform.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 好友请求表
 *
 * @TableName t_request_friend
 */
@TableName(value = "t_request_friend")
@Data
public class RequestFriendDO {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发起请求用户id
     */
    private Long requestUserId;

    /**
     * 接收id
     */
    private Long receiveUserId;

    /**
     * 处理状态（枚举：处理中:0 同意:1拒绝:2）
     */
    private Integer status;

    /**
     * 请求留言
     */
    private String requestNote;

    /**
     * 处理时间
     */
    private Date dealTime;

    /**
     * 回复
     */
    private String comment;

    /**
     * 创建日期
     */
    private Date createdTime;

    /**
     * 更新日期
     */
    private Date updateTime;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        RequestFriendDO other = (RequestFriendDO) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getRequestUserId() == null ? other.getRequestUserId() == null : this.getRequestUserId().equals(other.getRequestUserId()))
                && (this.getReceiveUserId() == null ? other.getReceiveUserId() == null : this.getReceiveUserId().equals(other.getReceiveUserId()))
                && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
                && (this.getRequestNote() == null ? other.getRequestNote() == null : this.getRequestNote().equals(other.getRequestNote()))
                && (this.getDealTime() == null ? other.getDealTime() == null : this.getDealTime().equals(other.getDealTime()))
                && (this.getComment() == null ? other.getComment() == null : this.getComment().equals(other.getComment()))
                && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
                && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getRequestUserId() == null) ? 0 : getRequestUserId().hashCode());
        result = prime * result + ((getReceiveUserId() == null) ? 0 : getReceiveUserId().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getRequestNote() == null) ? 0 : getRequestNote().hashCode());
        result = prime * result + ((getDealTime() == null) ? 0 : getDealTime().hashCode());
        result = prime * result + ((getComment() == null) ? 0 : getComment().hashCode());
        result = prime * result + ((getCreatedTime() == null) ? 0 : getCreatedTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", requestUserId=").append(requestUserId);
        sb.append(", receiveUserId=").append(receiveUserId);
        sb.append(", status=").append(status);
        sb.append(", requestNote=").append(requestNote);
        sb.append(", dealTime=").append(dealTime);
        sb.append(", comment=").append(comment);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}