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
export const sendMessageReq = (apiUrl,msgInfo) => {
    return http({
        url: apiUrl,
        method: 'post',
        data: msgInfo
    })
}