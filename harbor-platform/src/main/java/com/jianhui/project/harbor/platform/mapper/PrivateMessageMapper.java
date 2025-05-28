package com.jianhui.project.harbor.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jianhui.project.harbor.platform.entity.PrivateMessage;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
* @author wjh2
* @description 针对表【t_private_message(私聊消息)】的数据库操作Mapper
* @createDate 2025-05-04 18:25:04
* @Entity generator.domain.PrivateMessage
*/
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    /**
     * 分页查询历史消息，不包含撤回的消息
     */
    List<PrivateMessage>  pageHistoryMsg(Long userId,Long friendId,Long offset,Long size,Integer recallStateCode);

    /**
     * 查询离线消息
     * @param userId 接收
     * @param minId 已读消息id
     * @param minDate 从minDate开始查
     */
    List<PrivateMessage> getOfflineMsg(Long userId, Long minId, Date minDate,Integer recallStateCode);

    /**
     * 将已收到私聊信息设置为已读
     * @param userId 接收
     * @param friendId 发送
     * @param sentCode
     * @param readedCode
     */
    @Update("update t_private_message set status = #{readedCode} " +
            "where send_id = ${friendId} and recv_id = #{userId} and status = #{sentCode}")
    void updateStatusToReaded(Long userId, Long friendId, Integer sentCode, Integer readedCode);

    /**
     * 查询会话已读消息最大id
     * @param userId 发送
     * @param friendId 接收
     * @param readedCode
     * @return
     */
    Long getMaxReadedMsgId(Long userId, Long friendId, Integer readedCode);
}




