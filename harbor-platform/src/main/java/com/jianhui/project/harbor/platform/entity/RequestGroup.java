package com.jianhui.project.harbor.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 加入群请求表
 * @TableName t_request_group
 */
@TableName(value = "t_request_group")
@Data
public class RequestGroup {
    /**
     * 
     */
    private Long id;

    /**
     * 群id
     */
    private Long groupId;

    /**
     * 请求人id
     */
    private Long requestUserId;

    /**
     * 处理状态(枚举:未处理0，同意1，拒绝2)
     */
    private Integer status;

    /**
     * 请求留言
     */
    private String requestNote;

    /**
     * 处理人id
     */
    private Long dealUserId;

    /**
     * 处理结果原因，理由
     */
    private String comment;

    /**
     * 处理日期
     */
    private Date dealTime;

    /**
     * 创建日期
     */
    private Date createdTime;

    /**
     * 更新时间
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
        RequestGroup other = (RequestGroup) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGroupId() == null ? other.getGroupId() == null : this.getGroupId().equals(other.getGroupId()))
            && (this.getRequestUserId() == null ? other.getRequestUserId() == null : this.getRequestUserId().equals(other.getRequestUserId()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getRequestNote() == null ? other.getRequestNote() == null : this.getRequestNote().equals(other.getRequestNote()))
            && (this.getDealUserId() == null ? other.getDealUserId() == null : this.getDealUserId().equals(other.getDealUserId()))
            && (this.getComment() == null ? other.getComment() == null : this.getComment().equals(other.getComment()))
            && (this.getDealTime() == null ? other.getDealTime() == null : this.getDealTime().equals(other.getDealTime()))
            && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGroupId() == null) ? 0 : getGroupId().hashCode());
        result = prime * result + ((getRequestUserId() == null) ? 0 : getRequestUserId().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getRequestNote() == null) ? 0 : getRequestNote().hashCode());
        result = prime * result + ((getDealUserId() == null) ? 0 : getDealUserId().hashCode());
        result = prime * result + ((getComment() == null) ? 0 : getComment().hashCode());
        result = prime * result + ((getDealTime() == null) ? 0 : getDealTime().hashCode());
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
        sb.append(", groupId=").append(groupId);
        sb.append(", requestUserId=").append(requestUserId);
        sb.append(", status=").append(status);
        sb.append(", requestNote=").append(requestNote);
        sb.append(", dealUserId=").append(dealUserId);
        sb.append(", comment=").append(comment);
        sb.append(", dealTime=").append(dealTime);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}