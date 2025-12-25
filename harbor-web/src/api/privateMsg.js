import http from "./http.js";

/**
 * public class PrivateMessageVO {
 *     private Long id;
 *     private Long sendId;
 *     private Long recvId;
 *     private String content;
 *     private Integer type;
 *     private Integer status;
 *     private Date sendTime;
 * }
 */

/**
 * 发送消息
 */
export const sendMessageReq = (apiUrl, msgInfo) => {
  return http({
    url: apiUrl,
    method: "POST",
    data: msgInfo,
  });
};

/**
 * 发送私聊消息
 * @param {Object} data - PrivateMessageDTO
 */
export const sendPrivateMessage = (data) => {
  return http({
    url: "/message/private/send",
    method: "POST",
    data,
  });
};

/**
 * 撤回私聊消息
 * @param {number} id - 消息 ID
 */
export const recallPrivateMessage = (id) => {
  return http({
    url: `/message/private/recall/${id}`,
    method: "DELETE",
  });
};

/**
 * 拉取离线消息（通过 websocket 异步推送）
 * @param {number} minId - 最小消息 ID
 */
export const pullOfflinePrivateMessage = (minId) => {
  return http({
    url: "/message/private/pullOfflineMessage",
    method: "GET",
    params: { minId },
  });
};

/**
 * 设置会话中接收的消息为已读
 * @param {number} friendId - 好友 ID
 */
export const readPrivateMessage = (friendId) => {
  return http({
    url: "/message/private/readed",
    method: "PUT",
    params: { friendId },
  });
};

/**
 * 获取某个会话中已读消息的最大 ID
 * @param {number} friendId - 好友 ID
 */
export const getMaxReadedPrivateMessageId = (friendId) => {
  return http({
    url: "/message/private/maxReadedId",
    method: "GET",
    params: { friendId },
  });
};

/**
 * 查询聊天记录
 * @param {number} friendId - 好友 ID
 * @param {number} page - 页码
 * @param {number} size - 每页条数
 */
export const getPrivateMessageHistory = (friendId, page, size) => {
  return http({
    url: "/message/private/history",
    method: "GET",
    params: { friendId, page, size },
  });
};
