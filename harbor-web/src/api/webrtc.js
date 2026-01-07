import http from "./http.ts";

export const loadWebRTCConfig = () => {
  return http({
    url: "/webrtc/config",
    method: "get",
  });
};
