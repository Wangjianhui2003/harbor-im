import http from "./http.ts";

/**
 * 发送群聊消息
 * @param {Object} data - GroupMessageDTO
 */
export const sendGroupMessage = (data) => {
  return http({
    url: "/message/group/send",
    method: "POST",
    data,
  });
};

/**
 * 撤回消息
 * @param {number} id - 消息 ID
 */
export const recallGroupMessage = (id) => {
  return http({
    url: `/message/group/recall/${id}`,
    method: "DELETE",
  });
};

/**
 * 拉取离线消息（消息将通过 websocket 异步推送）
 * @param {number} minId - 最小消息 ID
 */
export const pullOfflineGroupMessage = (minId) => {
  return http({
    url: "/message/group/pullOfflineMessage",
    method: "GET",
    params: { minId },
  });
};

/**
 * 设置群聊消息为已读
 * @param {number} groupId - 群聊 ID
 */
export const readGroupMessage = (groupId) => {
  return http({
    url: "/message/group/readed",
    method: "PUT",
    params: { groupId },
  });
};

/**
 * 获取某条消息的已读用户 ID 列表
 * @param {number} groupId - 群聊 ID
 * @param {number} messageId - 消息 ID
 */
export const getReadedUsers = (groupId, messageId) => {
  return http({
    url: "/message/group/findReadedUsers",
    method: "GET",
    params: { groupId, messageId },
  });
};

/**
 * 查询聊天记录
 * @param {number} groupId - 群聊 ID
 * @param {number} page - 页码
 * @param {number} size - 每页条数
 */
export const getGroupHistoryMessages = (groupId, page, size) => {
  return http({
    url: "/message/group/history",
    method: "GET",
    params: { groupId, page, size },
  });
};
