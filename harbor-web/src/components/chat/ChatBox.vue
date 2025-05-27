<script setup>
//聊天对话框

import ChatInput from "./chatbox/ChatInput.vue";
import {computed, nextTick, reactive, ref} from "vue";
import useChatStore from "../../store/chatStore.js";
import {sendMessageReq} from "../../api/message.js";
import ChatMessageItem from "./chatbox/ChatMessageItem.vue";
import useUserStore from "../../store/userStore.js";

const props = defineProps({
  chat: {
    type: Object,
  }
})

const chatStore = useChatStore();
const userStore = useUserStore()

const chatEditor = ref(null)

const sendImageUrl = ref('')
const sendImageFile = ref('')
const placeholder = ref('')
const isReceipt = ref(true)
const showRecord = ref(false)
const showSide = ref(false)
const showHistory = ref(false)
//发送按钮锁定
const lockMessage = ref(false)
const showMinIdx = ref(0)
const isSending = ref(false)
const userInfo = reactive({})
const group = reactive({})
const groupMembers = ref([])
const reqQueue = ref([])

//发送信息的url
const sendMsgUrl = computed(() => {
  return `/message/${props.chat.type.toLowerCase()}/send`
})

//接收到发送事件，处理
const sendMessage = async (fullList) => {
  resetEditor()
  readedMessage()
  //TODO:判断禁言、封禁、回执消息
  for (let i = 0; i < fullList.length; i++) {
    let msg = fullList[i]
    switch (msg.type) {
      case "text":
        await sendTextMessage(msg.content, msg.atUserIds)
        break
    }
  }
}

const sendTextMessage = (sendText, atUserIds) => {
  return new Promise((resolve, reject) => {
    if (!sendText.trim()) {
      reject();
    }
    let msgInfo = {
      content: sendText,
      type: 0
    }
    //填充
    fillTargetId(msgInfo, props.chat.targetId)
    //群聊
    if (props.chat.type == "GROUP") {
      msgInfo.atUserIds = atUserIds;
      //TODO:isReceipt
      msgInfo.receipt = isReceipt.value;
    }
    lockMessage.value = true;
    msgInfo.selfSend = true;
    chatStore.insertMessage([msgInfo,props.chat])
    // 会话置顶
    moveChatToTop();
    // sendMessageRequest(msgInfo).then((m) => {
    //   //自己发送
    //   m.selfSend = true;
    //   chatStore.insertMessage([m,props.chat])
    //   // 会话置顶
    //   moveChatToTop();
    // }).finally(() => {
    //   scrollToBottom()
    //   isReceipt.value = false;
    // })
  })
}

//移动chat到顶部
const moveChatToTop = () => {

}

//移动到对话框底部
const scrollToBottom = () => {

}

//填充id
const fillTargetId = (msgInfo, targetId) => {
  //群聊
  if (props.chat.type == "GROUP") {
    msgInfo.groupId = targetId;
  } else {
    //私聊
    msgInfo.recvId = targetId;
  }
}

//发送请求到后端
const sendMessageRequest = (msgInfo) => {
  return new Promise((resolve, reject) => {
    return reject() //TODO:等后端写完
    // 请求入队列，防止请求"后发先至"，导致消息错序
    reqQueue.value.push({msgInfo, resolve, reject});
    processReqQueue();
  })
}

//处理请求队列
const processReqQueue = () => {
  if (reqQueue.value.length && !isSending.value){
    isSending.value = true;
    const reqData = reqQueue.value.unshift();
    sendMessageReq().then(data => {
      reqData.resolve(data)
    }).catch(error => {
      reqData.reject(error)
    }).finally(() => {
      isSending.value = false;
      // 发送下一条请求
      processReqQueue();
    })
  }
}

//重置输入区
const resetEditor = () => {
  nextTick(() => {
    console.log(chatEditor.value)
    chatEditor.value.clear()
    chatEditor.value.focus()
  })
}

const readedMessage = () => {
}


const mine = computed(() => {
  return userStore.userInfo.id
})

</script>

<template>
  <div class="chatbox">
    <div class="chatbox-header">
      {{ chat.showName }}
    </div>
    <div class="content">
      <ul>
        <li v-for="(msgInfo,idx) in chat.messages" :key="idx">
          <chat-message-item
              :mine="msgInfo.sendId == mine.id"
              :msgInfo="msgInfo" >
          </chat-message-item>
        </li>
      </ul>
    </div>
    <div class="chat-tool-bar">
    </div>
    <div class="input-box">
      <chat-input
          ref="chatEditor"
          @submit="sendMessage">
      </chat-input>
    </div>
  </div>

</template>

<style scoped lang="scss">
.chatbox {
  display: flex;
  flex-direction: column;
  height: 100vh;

  .chatbox-header {
    height: 50px;
    border-bottom: #dcdfe6 solid 1px;
  }

  .content {
    flex: 1;
    overflow: auto;
  }

  .input-box {
    min-height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

</style>