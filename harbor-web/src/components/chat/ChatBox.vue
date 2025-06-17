<script setup>
//聊天对话框

import ChatInput from "./chatbox_component/ChatInput.vue";
import {computed, nextTick, onMounted, provide, reactive, ref, watch} from "vue";
import useChatStore from "../../store/chatStore.js";
import {getMaxReadedPrivateMessageId, readPrivateMessage, sendMessageReq} from "../../api/privateMsg.js";
import ChatMessageItem from "./chatbox_component/ChatMessageItem.vue";
import useUserStore from "../../store/userStore.js";
import {CHATINFO_TYPE, MESSAGE_TYPE, MSG_CONTENT_TYPE} from "../../common/enums.js";
import {findGroup, findGroupMembers} from "../../api/group.js";
import {useGroupStore} from "../../store/groupStore.js";
import {readGroupMessage} from "../../api/groupMsg.js";
import {getUserInfo} from "../../api/user.js";
import useFriendStore from "../../store/friendStore.js";

const props = defineProps({
  chat: {
    type: Object,
  }
})

const chatStore = useChatStore();
const userStore = useUserStore()
const groupStore = useGroupStore()
const friendStore = useFriendStore()

const chatEditor = ref(null) //输入框

const sendImageUrl = ref('')
const sendImageFile = ref('') //
const showHistory = ref(false)  //历史消息
const placeholder = ref('') //
const isReceipt = ref(true) //是否是回执消息
const showVoiceRecorder = ref(false) //语音录制弹窗
const lockMessage = ref(false) //发送按钮锁定
const showMinIdx = ref(0) //下标低于showMinIdx的不显示
const reqQueue = ref([]) //等待发送队列
const isInBottom = ref(false) // 滚动条是否在底部
const newMsgBottom = ref(0) // 滚动条不在底部时新的消息数量
const isSending = ref(false) //是否正在发送

const userInfo = ref({}) //私聊时用户信息
const group = ref({}) //群聊时群信息

const groupMembers = ref([]) //


//判断当前会话是否被系统封禁
const isBanned = computed(() => {
  return (props.chat.type == CHATINFO_TYPE.GROUP && group.isBanned == true)
      || (props.chat.type == CHATINFO_TYPE.PRIVATE && userInfo.isBanned == true)
})

//发送信息的url
const sendMsgUrl = computed(() => {
  return `/message/${props.chat.type.toLowerCase()}/send`
})


//判断和当前会话是否是好友
const isFriend = computed(() => {
  return friendStore.isFriend(userInfo.value.id)
})

//返回好友信息(备注)
const friend = computed(() => {
  return friendStore.findFriend(userInfo.value.id)
})

//当前会话未读计数
const unreadCount = computed(() => {
  return props.chat.unreadCount
})


const isGroup = computed(() => {
  return props.chat.type === CHATINFO_TYPE.GROUP
})

const isPrivate = computed(() => {
  return props.chat.type === CHATINFO_TYPE.PRIVATE
})


const sendImageMessage = (file) => {

}

async function sendFileMessage(file) {

}

//接收到发送事件处理
const sendMessage = async (fullList) => {
  resetEditor()
  readedMessage()
  //TODO:判断禁言、封禁、回执消息
  if(isBanned.value){
    showBannedTip()
    return
  }
  for (const msg of fullList) {
    switch (msg.type) {
      case MSG_CONTENT_TYPE.TEXT:
        //文本
        await sendTextMessage(msg.content, msg.atUserIds)
        break
        //TODO:其他类型消息
      case MSG_CONTENT_TYPE.IMAGE:
        await sendImageMessage(msg.content.file);
        break;
      case MSG_CONTENT_TYPE.FILE:
        await sendFileMessage(msg.content.file);
        break;
    }
  }
}



//
const showBannedTip =() => {
  let msgInfo = {
    tmpId: generateId(),
    sendId: userStore.userInfo.id,
    sendTime: new Date().getTime(),
    type: MESSAGE_TYPE.TIP_TEXT
  }
  if (chat.type == CHATINFO_TYPE.PRIVATE) {
    msgInfo.recvId = userStore.userInfo.id
    msgInfo.content = "该用户已被管理员封禁,原因:" + userInfo.value.reason
  } else {
    msgInfo.groupId = group.value.id
    msgInfo.content = "本群聊已被管理员封禁,原因:" + group.value.reason
  }
  chatStore.insertMessage(msgInfo,props.chat)
}

// 生成临时id
const generateId = () => {
  return String(new Date().getTime()) + String(Math.floor(Math.random() * 1000));
}

//处理文本信息
const sendTextMessage = (sendText, atUserIds) => {
  return new Promise((resolve, reject) => {
    if (!sendText.trim()) {
      reject();
    }
    let msgInfo = {
      content: sendText,
      type: 0
    }
    //填充id
    fillTargetId(msgInfo, props.chat.targetId)
    //群聊
    if (props.chat.type == CHATINFO_TYPE.GROUP) {
      msgInfo.atUserIds = atUserIds;
      //TODO:isReceipt
      msgInfo.receipt = isReceipt.value;
    }
    lockMessage.value = true;
    //发送
    console.log('msg',msgInfo)
    sendMessageRequest(msgInfo).then((m) => {
      //是自己发送 TODO:优化发送延迟问题
      m.selfSend = true;
      chatStore.insertMessage(m,props.chat)
    }).finally(() => {
      scrollToBottom()
      //
      isReceipt.value = false;
      resolve()
    })
    // 会话置顶
    moveChatToTop();
  })
}

//移动chat到顶部
const moveChatToTop = () => {
  let chatIdx = chatStore.findChatIdx(props.chat)
  chatStore.moveTop(chatIdx)
}

//移动到对话框底部
const scrollToBottom = () => {
  nextTick(() => {
    let msgWindow = document.getElementById('msgWindow');
    msgWindow.scrollTop = msgWindow.scrollHeight
  })
}

//填充id
const fillTargetId = (msgInfo, targetId) => {
  //群聊
  if (props.chat.type == CHATINFO_TYPE.GROUP) {
    msgInfo.groupId = targetId;
  } else {
    //私聊
    msgInfo.recvId = targetId;
  }
}

//发送请求到后端
const sendMessageRequest = (msgInfo) => {
  return new Promise((resolve, reject) => {
    // 请求入队列，防止请求"后发先至"，导致消息错序
    reqQueue.value.push({msgInfo, resolve, reject});
    processReqQueue();
  })
}

//处理请求队列
const processReqQueue = () => {
  if (reqQueue.value.length && !isSending.value){
    isSending.value = true;
    const reqData = reqQueue.value.shift();
    sendMessageReq(sendMsgUrl.value,reqData.msgInfo)
    .then(data => {
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
    chatEditor.value.clear()
    chatEditor.value.focus()
  })
}

//发送请求：将该会话所有消息标为已读
const readedMessage = () => {
  if (props.chat.unreadCount === 0) return
  console.log('重置未读数:',unreadCount.value)
  chatStore.resetUnread(props.chat)
  //私聊
  if (props.chat.type == CHATINFO_TYPE.PRIVATE) {
    readPrivateMessage(props.chat.targetId)
  }else {
    //群聊
    readGroupMessage(props.chat.targetId)
  }
}

//监听到滚动事件时触发(onMounted时添加监听器到msgWindow)
const onScroll = () => {

}

//加载群、群成员信息
const loadGroup = (groupId) => {
  findGroup(groupId).then((groupVO) => {
    group.value = groupVO
    chatStore.updateChatFromGroup(groupVO)
    groupStore.updateGroup(groupVO)
  })
  findGroupMembers(groupId).then((members) => {
    groupMembers.value = members;
  })
}

//后端请求加载好友信息
const loadFriend = (friendId) => {
  getUserInfo(friendId).then((userVO) => {
    userInfo.value = userVO
    updateFriendInfo()
  })
}

//切换到私聊会话时更新信息
const updateFriendInfo = () => {
  if(isFriend.value){
    let friendInfo = JSON.parse(JSON.stringify(friend.value));
    friendInfo.headImage = userInfo.value.headImageThumb;
    friendInfo.friendNickname = userInfo.value.nickname;
    //有备注显示备注
    friendInfo.showNickname =
        friend.value.remarkNickname ? friend.value.remarkNickname : friend.value.friendNickname;
    chatStore.updateChatFromFriend(friendInfo)
    friendStore.updateFriend(friendInfo)
  }else {
    //已经不是好友
    chatStore.updateChatFromUser(userInfo.value)
  }
}

//将当前私聊该为已读的设为已读(多客户端同时在线)
const loadReaded = (friendId) => {
  getMaxReadedPrivateMessageId(friendId).then((maxId => {
    chatStore.readedMessage(friendId,maxId)
  }))
}

//监听chat切换，做刷新信息操作和清除输入栏等操作
watch(() => props.chat, async (newChat,oldChat) => {
  if (newChat.targetId > 0 && (!oldChat || newChat.type != oldChat.type || newChat.targetId != oldChat.targetId)) {
    userInfo.value = {}
    groupMembers.value = []
    group.value = {}
    if(newChat.type == CHATINFO_TYPE.GROUP) {
      loadGroup(props.chat.targetId)
    }else{
      loadFriend(props.chat.targetId)
      loadReaded(props.chat.targetId)
    }
    scrollToBottom()
    readedMessage()
    resetEditor();
    isReceipt.value = false;
    //TODO:完善刷新
  }
  }, {immediate: true,deep: true}
)

const chooseHeadImage = (msgInfo) => {
  if (isGroup.value){
    let member = groupMembers.value.find((m) => m.userId == msgInfo.sendId)
    return member ? member.headImage : "";
  }else {
    return msgInfo.sendId == userStore.userInfo.id ? userStore.userInfo.headImageThumb : props.chat.headImage
  }
}

const chooseShowName = (msgInfo) => {
  if (!msgInfo) {
    return ""
  }
  if (isGroup.value) {
    //群成员头像
    let member = groupMembers.value.find((m) => m.userId == msgInfo.sendId);
    return member ? member.showNickname : msgInfo.sendNickname || "";
  } else {
    //自己的或好友的头像
    return msgInfo.selfSend ? userStore.userInfo.nickname : props.chat.showName
  }
}

onMounted(() => {
  let msgWindow = document.getElementById('msgWindow');
  msgWindow.addEventListener('scroll',onScroll)
})


provide('chatBoxMethod',{
  sendMessageRequest,
  scrollToBottom,
  moveChatToTop
})

</script>

<template>
  <div class="chatbox">
    <div class="chatbox-header">
      {{ chat.showName }}
    </div>
    <div class="content" id="msgWindow">
      <ul>
        <li v-for="(msgInfo,idx) in chat.messages" :key="idx">
          <chat-message-item
              v-if="idx >= showMinIdx"
              :mine="msgInfo.sendId == userStore.userInfo.id"
              :headImage="chooseHeadImage(msgInfo)"
              :showName="chooseShowName(msgInfo)"
              :msgInfo="msgInfo" >
          </chat-message-item>
        </li>
      </ul>
    </div>
    <div class="input-box">
      <chat-input
          :chat="props.chat"
          ref="chatEditor"
          :is-group="isGroup"
          :friend="friend"
          :group="group"
          :group-members="groupMembers"
          :is-banned="isBanned"
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
  background-color: var(--light-white);

  .chatbox-header {
    line-height: 35px;
    text-align: center;
    height: 35px;
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