import http from "./http.js";
import { WEBRTC_MODE } from "../common/enums.js";

/**
 * WebRTC相关后端API
 */

// 呼叫视频通话
export const call = (userId, mode = WEBRTC_MODE.VIDEO, offer) => {
  return http({
    url: "/webrtc/private/call",
    method: "post",
    params: { userId, mode },
    data: offer,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
    },
  });
};

// 接受视频通话
export const accept = (userId, answer) => {
  return http({
    url: "/webrtc/private/accept",
    method: "post",
    params: { userId },
    data: answer,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
    },
  });
};

// 拒绝视频通话
export const reject = (userId) => {
  return http({
    url: "/webrtc/private/reject",
    method: "post",
    params: { userId },
  });
};

// 取消呼叫
export const cancel = (userId) => {
  return http({
    url: "/webrtc/private/cancel",
    method: "post",
    params: { userId },
  });
};

// 呼叫失败
export const failed = (userId, reason) => {
  return http({
    url: "/webrtc/private/failed",
    method: "post",
    params: { userId, reason },
  });
};

// 挂断
export const hangup = (userId) => {
  return http({
    url: "/webrtc/private/hangup",
    method: "post",
    params: { userId },
  });
};

// 同步 candidate
export const sendCandidate = (userId, candidate) => {
  return http({
    url: "/webrtc/private/candidate",
    method: "post",
    params: { userId },
    data: candidate,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
    },
  });
};

// 心跳
export const heartbeat = (userId) => {
  return http({
    url: "/webrtc/private/heartbeat",
    method: "post",
    params: { userId },
  });
};
