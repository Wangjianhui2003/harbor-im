<script setup>
import {ChatDotRound, User} from "@element-plus/icons-vue";
import {onMounted, onUnmounted, ref} from "vue";
import useMainStore from "../store/index.js";
import {useRoute, useRouter} from "vue-router";
import * as wsApi from "../connect/wssocket.js";
import useChatStore from "../store/chatStore.js";
import {CHATINFO_TYPE, CMD_TYPE, MESSAGE_TYPE} from "../common/enums.js";
import {ElMessage, ElMessageBox} from "element-plus";
import useUserStore from "../store/userStore.js";
import {pullOfflinePrivateMsg} from "../api/message.js";
import * as checkMsgType from "../common/checkMsgType.js";
import useFriendStore from "../store/friendStore.js";

/**
 * 主页
 */

const router = useRouter()
const route = useRoute()

const mainStore = useMainStore()
const chatStore = useChatStore()
const userStore = useUserStore()
const friendStore = useFriendStore()

//ws地址
const wsUrl = import.meta.env.VITE_WS_URL;

//重连标识
const reconnecting = ref(false)

//断线重连
const reconnectWS = () => {
  reconnecting.value = true
  // 重新加载一次个人信息，目的是为了保证网络已经正常且token有效
  userStore.loadUser().then(() => {
    // 断线重连
    ElMessage.error("连接断开，正在尝试重新连接...")
    wsApi.reconnect(wsUrl,sessionStorage.getItem("accessToken"))
  }).catch(error => {
    // 10s后重试
    setTimeout(() => reconnectWS(), 10000)
  })
}

//重连WS成功
const onReconnectWS = () => {
  // 重连成功
  reconnecting.value = false;
  // 重新加载群和好友
  const promises = [];
  promises.push(friendStore().loadFriend());
  // promises.push();TODO:reload group
  Promise.all(promises).then(() => {
    pullPrivateOfflineMsg()
    pullGroupOfflineMsg()
    ElMessage.success("重连成功")
  }).catch(error => {
    ElMessage.error("拉取离线信息失败")
    onExit()
  })
}

const onExit = () => {
  wsApi.close(3000)
  sessionStorage.removeItem("accessToken");
  location.href="/"
}

//加载store,初始化websocket
const init = () => {
  mainStore.loadAll().then(() => {
    wsApi.connect(wsUrl, sessionStorage.getItem("accessToken"))

    //连接成功的回调
    wsApi.onConnect(() => {
      if (reconnecting.value) {
        onReconnectWS()
      } else {
        console.log("ws登录成功，开始拉取离线消息")
        pullPrivateOfflineMsg()
        pullGroupOfflineMsg()
      }
    })

    //设置接收到消息的回调
    wsApi.onMessage((cmd, msgInfo) => {
      if (cmd === CMD_TYPE.FORCE_LOGOUT) {
        // 关闭ws
        wsApi.close(3000)
        // 异地登录，强制下线
        ElMessageBox.alert("您已在其他地方登录，将被强制下线", "强制下线", {
          confirmButtonText: "确定",
          callback: action => {
            location.href = "/";
          }
        })
      } else if (cmd === CMD_TYPE.PRIVATE_MESSAGE) {
        // 处理私聊消息
        handlePrivateMessage(msgInfo);
      } else if (cmd === CMD_TYPE.GROUP_MESSAGE) {
        // 处理群聊消息
        handleGroupMessage(msgInfo);
      } else if (cmd === CMD_TYPE.SYSTEM_MESSAGE) {
        // 处理系统消息
        handleSystemMessage(msgInfo);
      }
    })

    //设置ws关闭的回调函数
    wsApi.onClose((e) => {
      console.log(e);
      if (e.code != 3000) {
        // 断线重连
        reconnectWS();
      }
    })

  }).catch(error => {
    console.log("初始化失败",error);
  })
}

//拉取离线消息
const pullPrivateOfflineMsg = () => {
  //设置加载标识符
  chatStore.setLoadingPrivateMsgState(true)
  console.log(chatStore.privateMsgMaxId)
  pullOfflinePrivateMsg(chatStore.privateMsgMaxId).catch((err) => {
    console.log("拉取离线消息出错",err)
    chatStore.setLoadingPrivateMsgState(false)
  })
}

//TODO:拉取离线群聊消息
const pullGroupOfflineMsg = () => {
}

//插入私聊消息(有人发消息)
const insertPrivateMsg = (friend, msgInfo) => {
  let chatInfo = {
    type: CHATINFO_TYPE.PRIVATE,
    targetId: friend.id,
    showName: friend.friendNickname,
    headImage: friend.headImage
  }
  //打开会话
  chatStore.openChat(chatInfo)
  //插入信息
  chatStore.insertMessage([msgInfo,chatInfo])
  //TODO:私聊接收消息提示
}

//处理消息
const handlePrivateMessage = (msgInfo) => {
  // 标记这条消息是不是自己发的
  msgInfo.selfSend = msgInfo.sendId === userStore.userInfo.id
  let friendId = msgInfo.selfSend ? msgInfo.recvId : msgInfo.sendId
  //会话信息
  let chatInfo = {
    type: CHATINFO_TYPE.PRIVATE,
    targetId: friendId
  }

  //加载消息
  if(msgInfo.type == MESSAGE_TYPE.LOADING){
    chatStore.setLoadingPrivateMsgState(JSON.parse(msgInfo.content))
    return
  }

  // 已读消息
  if(msgInfo.type == MESSAGE_TYPE.READED){

  }

  // 消息回执处理,改消息状态为已读
  if(msgInfo.type == MESSAGE_TYPE.RECEIPT){

  }

  // 消息撤回
  if (msgInfo.type == MESSAGE_TYPE.RECALL) {
    chatStore.recallMessage([msgInfo,chatInfo])
    return;
  }

  // 新增好友
  if (msgInfo.type == MESSAGE_TYPE.FRIEND_NEW) {
    friendStore.addFriend(JSON.parse(msgInfo.content))
    return;
  }

  // (被)删除好友
  if(msgInfo.type == MESSAGE_TYPE.FRIEND_DEL) {
    friendStore.removeFriend(friendId)
  }

  //需要会话显示的消息
  if(checkMsgType.isNormal(msgInfo.type)
      || checkMsgType.isTip(msgInfo.type)
      || checkMsgType.isAction(msgInfo.type)) {
    let friend = loadFriendInfo(friendId);
    insertPrivateMsg(friend,msgInfo)
  }
}

//加载好友信息(好友给你发消息)
const loadFriendInfo = (friendId) => {
  let friend = friendStore.findFriend(friendId)
  if(!friend){
    friend = {
      id: friendId,
      friendNickname: "未知用户",
      headImage: ""
    }
  }
  return friend
}

//TODO:处理群聊消息
const handleGroupMessage = (msgInfo) => {

}

//TODO:处理系统消息
const handleSystemMessage = (msgInfo) => {

}

//路由
const goTo = (path) => {
  router.push(path)
}

onMounted(() => {
  //初始化
  init()
})

onUnmounted(() => {
  //关闭WebSocket
  wsApi.close()
})
</script>

<template>
  <el-container>
    <el-aside width="65px" class="navibar">
      <ul class="navilist">
        <li @click="goTo('/home/chat')" :class="{ active: route.path === '/home/chat'}">
          <el-icon>
            <ChatDotRound/>
          </el-icon>
        </li>
        <li @click="goTo('/home/friend')" :class="{ active: route.path === '/home/friend'}">
          <el-icon>
            <User/>
          </el-icon>
        </li>
        <li @click="goTo('/home/group')" :class="{ active: route.path === '/home/group'}">
          <el-icon>
            <Grid/>
          </el-icon>
        </li>
        <li @click="goTo('/home/setting')" :class="{ active: route.path === '/home/setting'}">
          <el-icon>
            <Setting/>
          </el-icon>
        </li>
      </ul>
    </el-aside>
    <el-main class="routerview">
      <div>
        <router-view></router-view>
      </div>
    </el-main>
  </el-container>
</template>

<style scoped>

.navibar {
  display: flex;
  justify-content: center;
  border-right: #dcdfe6 solid 1px;
}

.navilist {
  height: 100vh;
  background: #ffffff;
  text-align: center;
  padding-top: 10px;

  li {
    height: 45px;
    width: 45px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 9px;
    border-radius: 10px;

    .el-icon {
      font-size: 30px;
    }

    &:hover {
      background-color: #23d990;
      box-shadow: 0px 15px 20px rgba(46, 229, 157, 0.4);
      color: #fff;
      transition: all 0.3s ease 0s;
    }

    &.active {
      background-color: #23c483;
      box-shadow: 0px 15px 20px rgba(46, 229, 157, 0.4);
      color: #fff;
    }
  }
}

.routerview {
  padding: 0;
}
</style>