import {defineStore} from "pinia";
import {RTC_STATE} from "../common/enums.js";
import {getSelfInfo} from "../api/user.js";

// 用户信息
const useUserStore = defineStore('userStore', {
    state: () => ({
        userInfo: {},
        rtcInfo: {
            friend: {},  // 好友信息
            mode: "video", // 模式 video:视频 voice:语音
            state: RTC_STATE.FREE // FREE:空闲  WAIT_CALL:呼叫方等待 WAIT_ACCEPT: 被呼叫方等待  CHATING:聊天中
        }
    }),
    actions: {
        //设置自己信息
        setUserInfo(userInfo) {
            this.userInfo = userInfo
        },
        //清除
        clear() {
            this.userInfo = {};
            this.rtcInfo = {
                friend: {},
                mode: "video",
                state: RTC_STATE.FREE
            };
        },
        //加载自己信息
        async loadUser() {
            try {
                getSelfInfo().then((userInfo) => {
                    this.setUserInfo(userInfo)
                })
            } catch (err) {
                throw err
            }
        }
    }
})

export default useUserStore