import http from "./http.js";

export const loadWebRTCConfig = () => {
    return http({
        url: '/webrtc/config',
        method: 'get',
    })
}