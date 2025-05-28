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
export const sendMessageReq = (apiUrl,msgInfo) => {
    return http({
        url: apiUrl,
        method: 'POST',
        data: msgInfo
    })
}

/**
 * 拉取私聊离线消息
 */
export const pullOfflinePrivateMsg = (minId) => {
    return http({
        url: "message/private/pullOfflineMessage",
        method: 'GET',
        params: {minId}
    })
}
