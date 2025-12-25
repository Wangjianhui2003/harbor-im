import { defineStore } from "pinia";
import { ref } from "vue";
import { loadWebRTCConfig } from "../api/webrtc.js";
/**
 * 和webRTC相关的状态
 */

export const useWebRTCStore = defineStore("WebRTCStore", () => {
  /**
   * maxChannel
   * iceServers
   */
  const webRTCConfig = ref({});

  //设置配置
  const setConfig = (config) => {
    webRTCConfig.value = config;
  };

  const clear = () => {
    webRTCConfig.value = {};
  };

  //加载webrtc配置
  const loadConfig = async () => {
    loadWebRTCConfig()
      .then((webRTCConfig) => {
        console.log(webRTCConfig);
        setConfig(webRTCConfig);
        console.log("加载webrtc配置成功");
      })
      .catch((err) => {
        console.log("加载WebRTC配置出错", err);
      });
  };

  return { webRTCConfig, clear, setConfig, loadConfig };
});
