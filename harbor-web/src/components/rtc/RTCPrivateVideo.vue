<script setup>

import {computed, nextTick, onBeforeUnmount, onMounted, ref} from "vue";
import WebRTC from "../../common/webrtc.js";
import IMCamera from "../../common/camera.js";
import {MESSAGE_TYPE, WEBRTC_MODE, WEBRTC_STATE} from "../../common/enums.js";
import {ElMessage} from "element-plus";
import {useWebRTCStore} from "../../store/webRTCStore.js";
import {accept, call, cancel, failed, hangup, heartbeat, reject, sendCandidate} from "../../api/rtcPrivate.js";
import {findFriend} from "../../api/friend.js";
import {RTC_WAIT_TIMEOUT} from "../../common/constant.js";
import RTCPrivateCallPanel from "./RTCPrivateCallPanel.vue";
import HeadImage from "../common/HeadImage.vue";
import useUserStore from "../../store/userStore.js";

/**
 *视频语音通话面板
 */

//显示通话面板
const showRTCPanel = ref(false)

//本地媒体(摄像头,麦克风
const camera = new IMCamera()
const webRTC = new WebRTC()
//呼叫音频
const audio = new Audio()
//好友信息
const friend = ref({})

// 是否发起人
const isHost = ref(false)
// 状态: CLOSE:关闭  WAITING:等待呼叫或接听 CHATTING:聊天中 ERROR:出现异常
const state = ref(WEBRTC_STATE.CLOSE)
// 模式 VIDEO:视频聊 VOICE:语音聊天
const mode = ref(WEBRTC_MODE.VIDEO)
// 本地视频流
let localStream = null
// 对方视频流
let remoteStream = null
//通话时间
const chatTime = ref(0)
let chatTimer = null
let heartbeatTimer = null
let waitTimer = null
const candidates = ref([])

//ref
const localVideo = ref(null)
const remoteVideo = ref(null)

//是否是视频通话 false:语音通话
const isVideo = computed(() => {
  return mode.value === WEBRTC_MODE.VIDEO
})

//模式文本
const modeText = computed(() => {
  return isVideo.value ? "视频" : "语音"
})

//webRTC配置
const webRTCConfig = computed(() => {
  const webRTCStore = useWebRTCStore()
  let iceServers = webRTCStore.webRTCConfig.iceServers
  return {
    iceServers: iceServers,
  }
})

const isClosed = computed(() => {
  return state.value === WEBRTC_STATE.CLOSE
})

let offer = null

const candidatesReceived = []

//用rtcInfo打开RTC面板,如果是本人发起，调用call
const open = (rtcInfo) => {
  showRTCPanel.value = true
  mode.value = rtcInfo.mode
  isHost.value = rtcInfo.isHost
  friend.value = rtcInfo.friend
  //是自己发起的
  if (isHost.value) {
    console.log('主动发起RTC')
    onCall()
  }
}

//开始呼叫
const onCall = () => {
  if (!checkDevEnable()) {
    close()
    console.log('设备不支持')
    return
  }

  initRTC()

  startHeartbeat()

  openStream().then((stream) => {
    console.log('setStream', stream)
    webRTC.setStream(stream)
    state.value = WEBRTC_STATE.WAITING
    webRTC.createOffers().then(offer => {
      console.log('offer:', offer)
      call(friend.value.id, mode.value, offer).then(() => {
        //TODO:播放铃声
      }).catch(error => {
        console.log(error)
        close()
      })
    }).catch(error => {
      // 呼叫方必须能打开摄像头，否则无法正常建立连接
      console.log(error)
      close()
    })
  })
}

//加载配置、初始化监听器
const initRTC = () => {

  webRTC.init(webRTCConfig.value)

  webRTC.setupPeerConnection((stream) => {
    remoteStream = stream;
    nextTick(() => {
      remoteVideo.value.srcObject = remoteStream;
    })
  })

  webRTC.onIceCandidate((candidate) => {
    if (isChatting.value) {
      //直接发candidate
      sendCandidate(friend.value.id, candidate)
    } else {
      //保存起来
      candidates.value.push(candidate)
    }
  })

  webRTC.onICEStateChange((state) => {
    if (state === 'connected') {
      console.log('ice connected')
    } else if (state === 'disconnected') {
      console.log("ice disconnected")
    }
  })
}

//开始心跳，15秒一次
const startHeartbeat = () => {
  console.log("start webrtc heartbeat")
  heartbeatTimer && clearInterval(heartbeatTimer)
  heartbeatTimer = setInterval(() => {
    heartbeat(friend.value.id)
  }, 15000)
}

const openStream = async () => {
  if (isVideo.value) {
    //视频通话,打开摄像头和麦克风
    try {
      let stream = await camera.openVideo()
      localStream = stream;
      nextTick(() => {
        if (localVideo.value) {
          localVideo.value.srcObject = stream
          localVideo.value.muted = true;
        } else {
          console.log('摄像头未准备好')
        }
      })
      return stream
    } catch (error) {
      ElMessage.error('打开摄像头失败')
      console.log('打开摄像头失败:', error)
      throw error
    }
  } else {
    //语音通话
    try {
      let stream = await camera.openAudio()
      localStream = stream;
      localVideo.value.srcObject = stream
      return stream
    } catch (error) {
      ElMessage.error('打开麦克风失败')
      console.log('打开麦克风失败', error)
      throw error
    }
  }
}

//关闭处理
const close = () => {
  showRTCPanel.value = false
  camera.close()
  webRTC.close()
  audio.pause()
  chatTime.value = 0
  //清除定时器
  chatTimer && clearInterval(chatTimer)
  heartbeatTimer && clearInterval(heartbeatTimer)
  waitTimer && clearTimeout(waitTimer)
  chatTimer = null
  heartbeatTimer = null
  waitTimer = null
  state.value = WEBRTC_STATE.CLOSE
  candidates.value = []
}

//检查设备
const checkDevEnable = () => {
  if (!camera.isEnable()) {
    ElMessage.error("打开摄像头失败")
    return false;
  }
  if (!webRTC.isEnable()) {
    ElMessage.error("打开WebRTC失败")
    return false;
  }
  return true;
}

//收到信令
const onRTCPrivateMsg = (msgInfo) => {
  //关闭状态下直接忽略，除了发起通话
  if (msgInfo.type !== MESSAGE_TYPE.RTC_CALL_VIDEO &&
      msgInfo.type !== MESSAGE_TYPE.RTC_CALL_VOICE && isClosed.value) {
    return
  }
  switch (msgInfo.type) {
    case MESSAGE_TYPE.RTC_CALL_VOICE:
      onRTCCall(msgInfo, WEBRTC_MODE.VOICE)
      break;
    case MESSAGE_TYPE.RTC_CALL_VIDEO:
      onRTCCall(msgInfo, WEBRTC_MODE.VIDEO)
      break;
    case MESSAGE_TYPE.RTC_ACCEPT:
      onRTCAccept(msgInfo)
      break;
    case MESSAGE_TYPE.RTC_REJECT:
      onRTCReject(msgInfo)
      break;
    case MESSAGE_TYPE.RTC_CANCEL:
      onRTCCancel()
      break;
    case MESSAGE_TYPE.RTC_FAILED:
      onRTCFailed(msgInfo)
      break;
    case MESSAGE_TYPE.RTC_HANGUP:
      onRTCHangup()
      break;
    case MESSAGE_TYPE.RTC_CANDIDATE:
      onRTCCandidate(msgInfo)
      break;
  }
}


const onRTCCall = (msgInfo, mode0) => {
  offer = JSON.parse(msgInfo.content)
  console.log('offer:', offer)
  isHost.value = false
  mode.value = mode0
  findFriend(msgInfo.sendId).then(friendVO => {
    friend.value = friendVO
    state.value = WEBRTC_STATE.WAITING
    startHeartbeat()
    waitTimer = setTimeout(() => {
      failed(friend.value.id, "对方未接听")
      ElMessage.warning("你未接听")
      close()
    }, RTC_WAIT_TIMEOUT)
  })
};

const onRTCAccept = (msgInfo) => {
  if (msgInfo.selfSend) {
    //是接收方
    ElMessage.success("已在其他设备接听")
    close()
  } else {
    //是发送方收到了接受请求
    let answer = JSON.parse(msgInfo.content)
    console.log('answer', answer)
    webRTC.setRemoteSDP(answer).then(() => {
      state.value = WEBRTC_STATE.CHATTING
      console.log(webRTC.peerConnection.remoteDescription)
      addCachedICECandidate()
    }).catch(err => {
      console.log('error:set remote sdp', err)
    })
    //TODO:停止播放语音
    //发送candidate
    candidates.value.forEach(candidates => {
      sendCandidate(friend.value.id, candidates)
    })
    startChatTime()
  }
};

const addCachedICECandidate = () => {
  candidatesReceived.forEach(candidate => {
    console.log('add cached candidate', candidate)
    webRTC.addICECandidate(candidate)
  })
}

//开始通话计时
const startChatTime = () => {
  chatTime.value = 0
  chatTimer && clearInterval(chatTimer)
  chatTimer = setInterval(() => {
    chatTime.value++
  }, 1000)
}

const onRTCReject = (msgInfo) => {
  if (msgInfo.selfSend) {
    ElMessage.success('其他设备已经拒绝')
  } else {
    ElMessage.warning('对方拒绝了你的请求')
    close()
  }
};

const onRTCCancel = () => {
  ElMessage.warning('对方取消了呼叫')
  close()
};

const onRTCFailed = (msgInfo) => {
  ElMessage.error(msgInfo.content)
  close()
};

const onRTCHangup = () => {
  ElMessage.success('对方已挂断')
  close()
};

//添加candidate信息
const onRTCCandidate = (msgInfo) => {
  if (isChatting.value) {
    webRTC.addICECandidate(JSON.parse(msgInfo.content))
  } else {
    console.log('缓存candidate')
    candidatesReceived.push(JSON.parse(msgInfo.content))
  }
};

const onCloseDialog = () => {
  if (isChatting.value) {
    onHangup()
  } else if (isWaiting.value) {
    onCancel()
  } else {
    close()
  }
}

//手动接受
const onAccept = () => {
  if (!checkDevEnable()) {
    failed(friend.value.id, "对方设备不支持通话")
    close()
    return
  }
  //进入通话
  initRTC()

  showRTCPanel.value = true
  state.value = WEBRTC_STATE.CHATTING

  openStream().then(() => {
    webRTC.setStream(localStream)
    webRTC.createAnswers(offer).then((answer) => {
      console.log('answer', answer)
      accept(friend.value.id, answer)
      startChatTime()
      waitTimer && clearTimeout(waitTimer)
    })
  })
}

//手动拒绝
const onReject = () => {
  reject(friend.value.id)
  close()
  console.log('拒绝通话')
}

//手动挂断
const onHangup = () => {
  hangup(friend.value.id)
  ElMessage.success('您已挂断,通话结束')
  close()
}

//手动取消
const onCancel = () => {
  cancel(friend.value.id)
  ElMessage.success('您已取消通话')
  close()
}

const isChatting = computed(() => {
  return state.value === WEBRTC_STATE.CHATTING;
})

const isWaiting = computed(() => {
  return state.value === WEBRTC_STATE.WAITING;
})

defineExpose({
  open,
  onRTCPrivateMsg,
})

onMounted(() => {
  //TODO:初始化音频
  window.addEventListener('beforeunload', onCloseDialog)
})

onBeforeUnmount(() => {
  onCloseDialog()
  window.removeEventListener('beforeunload', onCloseDialog)
})

const title = computed(() => {
  return isWaiting.value ? '正在呼叫...' : modeText.value
})

const userStore = useUserStore()

</script>

<template>
  <el-dialog :modal="false" :close-on-click-modal="false" :center="true" v-model="showRTCPanel"
             :before-close="onCloseDialog" :title="title">
    <div v-show='isVideo' class="video-panel">
      <div class="remote-video">
        <video ref="remoteVideo" autoplay class="video"></video>
        <div class="local-video">
          <video ref="localVideo" autoplay class="video"></video>
        </div>
      </div>
      <button>挂断</button>
    </div>
    <div v-show='!isVideo' class='voice-panel'>
      <div>
        <head-image :url="userStore.userInfo.headImageThumb"/>
      </div>
      <div>
        <head-image :url="friend.headImage"/>
      </div>
    </div>
  </el-dialog>
  <RTCPrivateCallPanel v-if="!isHost && isWaiting" :friend="friend" :modeText="modeText"
                       @acceptRTCPrivateEvent="onAccept" @rejectRTCPrivateEvent="onReject"></RTCPrivateCallPanel>
</template>

<style scoped lang="scss">

.voice-panel {
  display: flex;
  align-items: center;
  justify-content: space-evenly;
}

.video-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.local-video {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 1;
  width: 160px
}

.remote-video {
  height: 487.5px;
  width: 650px;
  position: relative;
}

.video {
  height: auto;
  width: 100%;
}

</style>