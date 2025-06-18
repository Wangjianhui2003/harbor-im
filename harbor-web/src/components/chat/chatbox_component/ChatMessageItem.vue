<script setup>

import {computed, ref} from "vue";
import {MESSAGE_TYPE, MSG_INFO_LOAD_STATUS, MSG_ITEM_OP} from "../../../common/enums.js";
import * as dateUtil from "../../../common/date.js";
import * as checkMsgType from "../../../common/checkMsgType.js";
import HeadImage from "../../common/HeadImage.vue";

/**
 * 消息组件(聊天框里的一条条消息包括提示)
 */

const props = defineProps({
  msgInfo: {
    type: Object  //消息
  },
  mine: {
    type: Boolean //自己的消息
  },
  headImage: {
    type: String,
  },
  showName: {
    type: String //显示名称
  },
  groupMembers: {
    type: Array //群成员
  },
  haveRightMenu: {
    type: Boolean, //是否右键有菜单
    default: true
  }
})

const audioPlayState = ref('STOP')

//消息在加载
const loading = computed(() => {
  return props.msgInfo.loadStatus && props.msgInfo.loadStatus === 'loading'
})

//右键菜单栏项目
const menuItems = computed(() => {
  let items = []
  items.push({
    key: MSG_ITEM_OP.DELETE,
    name: '删除'
  })
  if (props.msgInfo.selfSend && props.msgInfo.id > 0) {
    items.push({
      key: MSG_ITEM_OP.RECALL,
      name: '撤回',
    })
  }
  return items
})

const isNormal = () => {
  return checkMsgType.isNormal(props.msgInfo.type) || checkMsgType.isAction(props.msgInfo.type)
}

const selfSend = computed(() => {
  return props.msgInfo.selfSend
})

//是否是交互类消息
const isAction = computed(() => {
  return checkMsgType.isAction(props.msgInfo.type)
})

const isLoading = computed(() => {
  return props.msgInfo.loadStatus && props.msgInfo.loadStatus === MSG_INFO_LOAD_STATUS.LOADING
})

const isLoadingFail = computed(() => {
  return props.msgInfo.loadStatus && props.msgInfo.loadStatus === MSG_INFO_LOAD_STATUS.FAIL
})

const data = computed(() => {
  return JSON.parse(props.msgInfo.content)
})

const fileSize = computed(() => {
  let size = data.value.size;
  if (size > 1024 * 1024) {
    return Math.round(size / 1024 / 1024) + "M";
  }
  if (size > 1024) {
    return Math.round(size / 1024) + "KB";
  }
  return size + "B";
})

const isFileMsg = computed(() => {
  return props.msgInfo.type === MESSAGE_TYPE.FILE
})

const maxWidth = 300
const maxHeight = 200

const imageRef = ref(null)
const imageThumb = ref({
  width: 'auto',
  height: 'auto',
})

const onLoadImage = () => {
  const img = imageRef.value
  const naturalWidth = img.naturalWidth
  const naturalHeight = img.naturalHeight

  // 计算缩放比例
  const scaleW = maxWidth / naturalWidth
  const scaleH = maxHeight / naturalHeight
  const scale = Math.min(scaleW, scaleH, 1) // 不放大，只缩小

  imageThumb.value = {
    width: `${naturalWidth * scale}px`,
    height: `${naturalHeight * scale}px`,
  }
}
</script>

<template>
  <div class="msg-item">
    <div v-if="props.msgInfo.type === MESSAGE_TYPE.TIP_TEXT" class="tip">
      {{ props.msgInfo.content }}
    </div>
    <div v-else-if="props.msgInfo.type === MESSAGE_TYPE.TIP_TIME" class="time-tip">
      {{ dateUtil.toTimeText(props.msgInfo.sendTime) }}
    </div>
    <div v-else-if="isNormal" class="normal" :class="{selfSendRow : selfSend}">
      <div>
        <head-image
            :size="36"
            :url="props.headImage"
            :name="props.showName"
            :id="props.msgInfo.sendId" >
        </head-image>
      </div>
      <div class="content-container" :class="{selfSendContent : selfSend}">
        <div class="send-info" :class="{selfSendRow : selfSend}">
          <div class="name">
            {{props.showName}}
          </div >
          <div class="send-time">
            {{dateUtil.toTimeText(props.msgInfo.sendTime)}}
          </div>
        </div>
        <div class="content" :class="{selfSendBubble : selfSend,fileMsg : isFileMsg}">
          <div class="text" v-if="props.msgInfo.type === MESSAGE_TYPE.TEXT">
            {{ props.msgInfo.content}}
          </div>
          <div class="action-content" v-if="isAction">
            <img class="rtc-icon" v-if="props.msgInfo.type === MESSAGE_TYPE.ACT_RT_VOICE" src="../../../assets/msgIcon/phone-msg.svg" alt="语音通话" title="重新发起语音通话">
            <img class="rtc-icon" v-if="props.msgInfo.type === MESSAGE_TYPE.ACT_RT_VIDEO" src="../../../assets/msgIcon/video-msg.svg" alt="视频通话" title="重新发起视频通话">
            {{ props.msgInfo.content}}
          </div>
          <div class="file" v-if="props.msgInfo.type === MESSAGE_TYPE.FILE">
            <div class="file-msg-box" v-loading="isLoading">
              <div class="file-info">
                <el-link class="file-name-text" :href="data.url" :underline="true" target="_blank" type="primary">{{data.name}}</el-link>
                <div class="file-size-text"> {{fileSize}} </div>
              </div>
              <div> <svg class="file-msg-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg> </div>
            </div>
            <span v-if="isLoadingFail" class="send-fail">发送失败!</span>
          </div>
          <div class="image" v-if="props.msgInfo.type === MESSAGE_TYPE.IMAGE">
            <div class="img-box" v-loading="isLoading">
              <img ref="imageRef" class='imageThumb' :src="data.originUrl" alt="图片" loading="lazy" @load="onLoadImage" :style="imageThumb">
            </div>
            <span class="send-fail" v-if="isLoadingFail">发送失败!</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">

.imageThumb{
  width: auto;
  height: auto;
}

.send-fail{
  color: #e30000;
  font-size: 12px;
}

.file-name-text{
  font-size: 15px;
}

.file-size-text{
  color: gray;
  font-size: 12px;
}

.file-info{
  margin-right: 30px;
}

.file-msg-box{
  height: auto;
  width: auto;
  display: flex;
  flex-direction: row;
  align-items: center;
}

.file-msg-icon{
  width: 50px;
  color: #48a6ff;
}

.action-content{
  display: flex;
  align-items: center;
}

.rtc-icon{
  margin-right: 5px;
  cursor: pointer;
}

.content{
  background-color: #94C2ED;
  padding: 10px;
  border-radius: 9px;
  max-width: 60%;
  overflow-wrap: break-word;
}

.selfSendBubble{
  background-color: var(--bubble-green);
}


.fileMsg{
  background-color: white;
}

.name{
  font-size: 14px;
}

.tip{
  margin: 45px 0;
  text-align: center;
  color: gray;
  font-size: 12px;
}

.time-tip {
  margin: 45px 0;
  text-align: center;
  color: gray;
  font-size: 12px;
}

.msg-item{
  margin-top: 18px;
}

.send-info{
  height: 20px;
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.send-time{
  color: gray;
  font-size: 11px;
  margin: 0 20px
}

.normal {
  margin: 10px;
  display: flex;
}

.selfSendRow{
  flex-direction: row-reverse;
}

.content-container{
  display: flex;
  flex-direction: column;
  align-items: start;
  margin: 0 10px;
  width: 100%;
}

.selfSendContent{
  align-items: flex-end;
}

.send-info{
  display: flex;
}


</style>