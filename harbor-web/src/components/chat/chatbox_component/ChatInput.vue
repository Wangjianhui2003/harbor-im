<script setup>

import {inject, nextTick, onMounted, onUnmounted, ref} from "vue";
import EmojiPicker from "vue3-emoji-picker";
import mitter from "../../../common/eventBus.js";
import {ElMessage} from "element-plus";
import {MESSAGE_STATUS, MESSAGE_TYPE, MSG_INFO_LOAD_STATUS, WEBRTC_MODE} from "../../../common/enums.js";
import FileUpload from "../../common/FileUpload.vue";
import {FILE_MSG_MAX_SIZE, IMAGE_MSG_MAX_SIZE} from "../../../common/constant.js";
import useUserStore from "../../../store/userStore.js";
import useChatStore from "../../../store/chatStore.js";
import {sendMessageReq} from "../../../api/privateMsg.js";

const props = defineProps({
  ownerId: {
    type: Number,
  },
  isGroup: {
    type: Boolean,
  },
  friend: {
    type: Object
  },
  group: {
    type: Object
  },
  groupMembers: {
    type: Array,
  },
  isBanned: {
    type: Boolean,
  },
  chat: {
    type: Object
  },
})

const emit = defineEmits(['submit','sendFileMsg'])

const userStore = useUserStore()
const chatStore = useChatStore()

//输入区
const content = ref(null)

const imageList = ref([])
const fileList = ref([])
const currentId = ref(0)
const atSearchText = ref(null)
const compositionFlag = ref(false)
const atIng = ref(false)
const isInputEmpty = ref(true)
const changeStored = ref(true)
const blurRange = ref(null)

//自定义enter
const onKeydown = (e) => {
  //enter
  if (e.keyCode === 13) {
    e.preventDefault()
    e.stopPropagation()
    if (e.ctrlKey) {
      //ctrl + enter换行 TODO:不能连续换行bug
      let line = newLine()
      let after = document.createTextNode('\u00A0')
      line.appendChild(after)
      selectElement(line.childNodes[0], 0)
    } else {
      //正在输入不要提交(适配中文输入法)
      if (compositionFlag.value) {
        return
      }
      submit();
    }
    return
  }
  // 删除键
  if (e.keyCode === 8) {
    console.log("delete")
    // 等待dom更新
    setTimeout(() => {
      let s = content.value.innerHTML.trim();
      // 空dom时，需要刷新dom
      console.log(s);
      if (s === '' || s === '<br>' || s === '<div>&nbsp;</div>') {
        // 拼接随机长度的空格，以刷新dom
        empty();
        isInputEmpty.value = true;
        selectElement(content.value);
      } else {
        isInputEmpty.value = false;
      }
    })
  }
}

//
const selectElement = (element, endOffset) => {
  let selection = window.getSelection();
  // 插入元素可能不是立即执行的，vue可能会在插入元素后再更新dom
  nextTick(() => {
    let t1 = document.createRange();
    t1.setStart(element, 0);
    t1.setEnd(element, endOffset || 0);
    if (element.firstChild) {
      t1.selectNodeContents(element.firstChild);
    }
    t1.collapse();
    selection.removeAllRanges();
    selection.addRange(t1);
    // 需要时自动聚焦
    if (element.focus) {
      element.focus();
    }
  })
}

//换行
const newLine = () => {
  let selection = window.getSelection();
  let range = selection.getRangeAt(0);
  let divElement = document.createElement('div');
  let endContainer = range.endContainer;
  let parentElement = endContainer.parentElement;
  if (parentElement.parentElement === content.value) {
    divElement.innerHTML = endContainer.textContent.substring(range.endOffset).trim();
    endContainer.textContent = endContainer.textContent.substring(0, range.endOffset);
    // 插入到当前div（当前行）后面
    parentElement.insertAdjacentElement('afterend', divElement);
  } else {
    divElement.innerHTML = '';
    content.value.append(divElement);
  }
  return divElement;
}

//发送(触发时间，让父组件处理
const submit = () => {
  console.log(content.value.innerHTML)
  let nodes = content.value.childNodes
  let fullList = [];
  let tempText = '';
  let atUserIds = [];
  let each = (nodes) => {
    for (let i = 0; i < nodes.length; i++) {
      //TODO:处理图片at等数据
      let node = nodes[i]
      if (!node) {
        continue;
      }
      //纯文本
      if (node.nodeType === 3) {
        tempText += html2Escape(node.textContent);
        continue;
      }
      let nodeName = node.nodeName.toLowerCase();
      //跳过脚本
      if (nodeName === 'script') {
        continue;
      } else if (nodeName === 'div') {
        tempText += '\n';
        each(node.childNodes);
      }
    }
  }
  each(nodes)
  let text = tempText.trim();
  //文本非空
  if (text !== '') {
    fullList.push({
      type: 'text',
      content: text,
      atUserIds: atUserIds
    })
  }
  console.log("submit", fullList)
  emit('submit', fullList)
}

//转义,防止xss攻击
const html2Escape = (strHtml) => {
  return strHtml.replace(/[<>&"]/g, function (c) {
    return {
      '<': '&lt;',
      '>': '&gt;',
      '&': '&amp;',
      '"': '&quot;'
    }[c];
  });
}

//输入结束时触发
const onCompositionend = (e) => {
  compositionFlag.value = false
  onEditorInput(e)
}

//编辑器输入时
const onEditorInput = (e) => {
  isInputEmpty.value = false
}

//清空
const clear = () => {
  empty();
  imageList.value = [];
  fileList.value = [];
}

//清空
const empty = () => {
  content.value.innerHTML = "";
  let line = newLine();
  let after = document.createTextNode('\u00A0');
  line.appendChild(after);
  nextTick(() => selectElement(after));
}

//聚焦
const focus = () => {
  content.value.focus()
}

//暴露方法
defineExpose({
  focus,
  clear
})

let selection = null

function saveSelection() {
  const sel = window.getSelection()
  if (sel.rangeCount > 0) {
    selection = sel.getRangeAt(0)
  }
}

//选择emoji
function onSelectEmoji(emoji) {
  if (!content.value) return

  // 聚焦回 contenteditable
  content.value.focus()

  const sel = window.getSelection()
  sel.removeAllRanges()

  // 如果之前保存了 range，就使用它
  if (selection) {
    sel.addRange(selection)
  }

  // 插入 emoji
  const range = sel.getRangeAt(0)
  const textNode = document.createTextNode(emoji.i)
  range.deleteContents() // 删除选中内容
  range.insertNode(textNode)

  // 移动光标到 emoji 后
  range.setStartAfter(textNode)
  range.collapse(true)

  sel.removeAllRanges()
  sel.addRange(range)

  // 更新保存的 range
  saveSelection()
}

const showEmojiPicker = ref(false)
//emojiPicker ref
const emojiPicker = ref(null)
//表情按钮ref
const emoteBtn = ref(null)

//发生点击事件时处理picker的关闭
const closeEmojiPicker = (e) => {
  //未开启
  if (!showEmojiPicker.value) {
    return
  }
  //在Picker内部
  if (emojiPicker.value && emojiPicker.value.$el.contains(e.target)) {
    return
  }
  //是按钮
  if (emoteBtn.value && emoteBtn.value.contains(e.target)) {
    return
  }
  showEmojiPicker.value = false
}

const openPrivateVideo = (mode) => {
  //检验是否被封禁
  if (props.isBanned) {
    showBannedTip()
    return
  }

  let rtcInfo = {
    mode: mode,
    isHost: true,
    friend: props.friend
  }
  mitter.emit("openPrivateVideoEvent", rtcInfo);
}

//TODO:显示通话用户被封禁提示
const showBannedTip = () => {
  ElMessage.error("该用户已经被封禁")
}

// 在组件挂载时添加事件监听器
onMounted(() => {
  document.addEventListener('click', closeEmojiPicker);
});

// 在组件卸载时移除事件监听器
onUnmounted(() => {
  document.removeEventListener('click', closeEmojiPicker);
});

const chatBoxMethod = inject('chatBoxMethod')

const fillTargetId = (msgInfo) => {
  if (props.isGroup){
    msgInfo.groupId = props.group.id
  }else {
    msgInfo.recvId = props.friend.id
  }
}

const generateRandomId = () => {
  return String(new Date().getTime()) + String(Math.floor(Math.random() * 1000));
}

const onUploadImageBefore = (file) => {
  if (props.isBanned){
    showBannedTip()
    return
  }
  let url = URL.createObjectURL(file)
  //预览
  let data = {
    thumbUrl: url,
    originUrl: url
  }
  let msgInfo = {
    id: 0,
    tmpId: generateRandomId(),
    sendId: userStore.userInfo.id,
    content: JSON.stringify(data),
    sendTime: new Date().getTime(),
    selfSend: true,
    type: MESSAGE_TYPE.IMAGE,
    loadStatus: "loading",
    readedCount: 0,
    status: MESSAGE_STATUS.UNSEND
  }
  fillTargetId(msgInfo)
  //先插入，上传成功后再请求后端
  chatStore.insertMessage(msgInfo,props.chat)
  chatBoxMethod.scrollToBottom()
  chatBoxMethod.moveChatToTop()
  //用file透传
  file.msgInfo = msgInfo
  file.chat = props.chat

}

const onUploadImageSuccess = (data,file) => {
  let msgInfo = JSON.parse(JSON.stringify(file.msgInfo))
  msgInfo.content = JSON.stringify(data)
  chatBoxMethod.sendMessageRequest(msgInfo).then(m => {
    msgInfo.loadStatus = MSG_INFO_LOAD_STATUS.OK
    msgInfo.id = m.id
    chatStore.insertMessage(msgInfo,file.chat)
  })
}

const onUploadImageFail = (err,file) => {
  let msgInfo = JSON.parse(JSON.stringify(file.msgInfo));
  msgInfo.loadStatus = MSG_INFO_LOAD_STATUS.FAIL
  chatStore.insertMessage(msgInfo,file.chat)
}

//文件上传前准备(插入到聊天框展示)
const onUploadFileBefore = (file) => {
  if (props.isBanned){
    showBannedTip()
    return
  }
  let url = URL.createObjectURL(file)
  let data = {
    name: file.name,
    size: file.size,
    url: url
  }
  let msgInfo = {
    id: 0,
    tmpId: generateRandomId(),
    sendId: userStore.userInfo.id,
    content: JSON.stringify(data),
    sendTime: new Date().getTime(),
    selfSend: true,
    type: MESSAGE_TYPE.FILE,
    loadStatus: "loading",
    readedCount: 0,
    status: MESSAGE_STATUS.UNSEND
  }
  fillTargetId(msgInfo)
  //先插入，上传成功后再请求后端
  chatStore.insertMessage(msgInfo,props.chat)
  chatBoxMethod.scrollToBottom()
  chatBoxMethod.moveChatToTop()
  //用file透传
  file.msgInfo = msgInfo
  file.chat = props.chat
}

//文件上传成功后：发消息到后端，改变原消息状态
const onUploadFileSuccess = (url,file) => {
  let data = {
    name: file.name,
    size: file.size,
    url: url
  }
  let msgInfo = JSON.parse(JSON.stringify(file.msgInfo));
  msgInfo.content = JSON.stringify(data);
  //TODO:receipt
  chatBoxMethod.sendMessageRequest(msgInfo).then(m => {
    msgInfo.loadStatus = MSG_INFO_LOAD_STATUS.OK
    msgInfo.id = m.id
    chatStore.insertMessage(msgInfo,file.chat)
  })
}

//文件上传失败，将原消息状态修改
const onUploadFileFail = (err,file) => {
  let msgInfo = JSON.parse(JSON.stringify(file.msgInfo))
  msgInfo.loadStatus = MSG_INFO_LOAD_STATUS.FAIL
  chatStore.insertMessage(msgInfo,props.chat)
}

//ref
const imageUploader = ref(null)

//ref
const fileUploader = ref(null)

//image msg type
const imageTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp', 'image/gif']


</script>

<template>
  <div class="chat-input-area">
    <div class="input-outer">
      <div class="input" contenteditable="true" ref="content" @keydown="onKeydown"
           @compositionstart="compositionFlag=true" @compositionend="onCompositionend" @keyup="saveSelection" @mouseup="saveSelection" @focus="saveSelection">
      </div>
      <div class="option">
        <img title='表情' src="../../../assets/input/emote.svg" alt="表情" class="icon emote"
             @click="showEmojiPicker=!showEmojiPicker" ref="emoteBtn">
        <file-upload title="发送图片" ref="imageUploader" :url="'/image/upload'" @before="onUploadImageBefore"
                     @upload-success="onUploadImageSuccess" @upload-fail="onUploadImageFail"
                     :max-size="IMAGE_MSG_MAX_SIZE" :file-types="imageTypes">
          <img src="../../../assets/input/image.svg" alt="图片" class="icon">
        </file-upload>
        <img title="发送语音" src="../../../assets/input/record.svg" alt="语音" class="icon">
        <file-upload title="发送文件" ref="fileUploader" :url="'/file/upload'" @before="onUploadFileBefore"
                     @upload-success="onUploadFileSuccess" @upload-fail="onUploadFileFail"
                     :max-size="FILE_MSG_MAX_SIZE">
          <img src="../../../assets/input/file.svg" alt="文件" class="icon">
        </file-upload>
        <img title="音频通话" src="../../../assets/input/phone-call.svg" alt="电话" class="icon"
             @click="openPrivateVideo(WEBRTC_MODE.VOICE)">
        <img title="视频通话" src="../../../assets/input/video.svg" alt="视频" v-if="!isGroup" class="icon"
             @click="openPrivateVideo(WEBRTC_MODE.VIDEO)">
        <img title='群视频通话' src="../../../assets/input/video.svg" alt="视频" v-if="isGroup" class="icon"
             @click="console.log('实现中')">
        <EmojiPicker :native="true" @select="onSelectEmoji" v-show="showEmojiPicker" class="emote-picker"
                     ref="emojiPicker"/>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">

.option {
  width: 20%;
  display: flex;
  align-items: center;

  position: relative;
}

.emote-picker {
  position: absolute;
  bottom: 40px;
  right: -15px;
}

.icon {
  cursor: pointer;
  height: 20px;
  width: 20px;
  margin: 0px 10px;
}

.emote {
  position: relative;
}

.chat-input-area {
  min-height: 100%;
  width: 100%;
  position: relative;
  background-color: var(--theme-light-gray);

  .input-outer {
    position: absolute;
    left: 11px;
    right: 0;
    bottom: 10px;
    min-height: 40px;
    width: 98%;
    padding: 12px;
    border: solid 1px #ddd;
    border-radius: 10px;
    background-color: white;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;

    display: flex;
    align-items: center;
  }

  .input {
    height: 100%;
    max-height: 350px;
    overflow: auto;
    flex: 1;
    outline: none;
    min-width: 0;
  }

}


</style>