<script setup>
import { ChatDotRound, User } from "@element-plus/icons-vue";
import { onMounted, onUnmounted, ref } from "vue";
import useMainStore from "../store/index.js";
import { useRoute, useRouter } from "vue-router";
import * as wsApi from "../connect/wssocket.js";
import useChatStore from "../store/chatStore.js";
import { CHATINFO_TYPE, CMD_TYPE, MESSAGE_TYPE } from "../common/enums.js";
import { ElMessage, ElMessageBox } from "element-plus";
import useUserStore from "../store/userStore.js";
import { pullOfflinePrivateMessage } from "../api/privateMsg.js";
import * as checkMsgType from "../common/checkMsgType.js";
import useFriendStore from "../store/friendStore.js";
import { useGroupStore } from "../store/groupStore.js";
import { pullOfflineGroupMessage } from "../api/groupMsg.js";
import RTCPrivateVideo from "../components/rtc/RTCPrivateVideo.vue";
import mitter from "../common/eventBus.js";

/**
 * 主页
 */

const router = useRouter();
const route = useRoute();

const mainStore = useMainStore();
const chatStore = useChatStore();
const userStore = useUserStore();
const friendStore = useFriendStore();
const groupStore = useGroupStore();

//rtcPrivateVideo ref
const rtcPrivateVideo = ref(null);

//ws地址
const wsUrl = import.meta.env.VITE_WS_URL;
// const wsUrl = "wss://117.72.198.67:8101/im";

//重连标识
const reconnecting = ref(false);

//断线重连
const reconnectWS = () => {
  reconnecting.value = true;
  // 重新加载一次个人信息，目的是为了保证网络已经正常且token有效
  userStore
    .loadUser()
    .then(() => {
      // 断线重连
      ElMessage.error("连接断开，正在尝试重新连接...");
      wsApi.reconnect(wsUrl, sessionStorage.getItem("accessToken"));
    })
    .catch((error) => {
      // 10s后重试
      setTimeout(() => reconnectWS(), 10000);
    });
};

//重连WS成功
const onReconnectWS = () => {
  // 重连成功
  reconnecting.value = false;
  // 重新加载群和好友
  const promises = [];
  promises.push(friendStore.loadFriend());
  promises.push(groupStore.loadGroups());
  Promise.all(promises)
    .then(() => {
      pullPrivateOfflineMsg();
      pullGroupOfflineMsg();
      ElMessage.success("重连成功");
    })
    .catch((error) => {
      ElMessage.error("拉取离线消息失败");
      onExit();
    });
};

const onExit = () => {
  wsApi.close(3000);
  sessionStorage.removeItem("accessToken");
  location.href = "/";
};

//加载store,初始化websocket
const init = () => {
  mainStore
    .loadAll()
    .then(() => {
      console.log("WebSocketURL:", wsUrl);
      wsApi.connect(wsUrl, sessionStorage.getItem("accessToken"));

      //连接成功回调
      wsApi.onConnect(() => {
        if (reconnecting.value) {
          //是重连
          onReconnectWS();
        } else {
          //是第一次登录
          console.log("开始拉取离线消息");
          pullPrivateOfflineMsg();
          pullGroupOfflineMsg();
        }
      });

      //设置接收到消息的回调
      wsApi.onMessage((cmd, msgInfo) => {
        if (cmd === CMD_TYPE.FORCE_LOGOUT) {
          // 关闭ws
          wsApi.close(3000);
          // 异地登录，强制下线
          ElMessageBox.alert("您已在其他地方登录，将被强制下线", "强制下线", {
            confirmButtonText: "确定",
            callback: (action) => {
              location.href = "/";
            },
          });
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
      });
      //设置ws关闭的回调函数
      wsApi.onClose((e) => {
        console.log(e);
        if (e.code != 3000) {
          // 断线重连
          reconnectWS();
        }
      });
    })
    .catch((error) => {
      console.log("初始化失败", error);
    });
};

//拉取离线消息
const pullPrivateOfflineMsg = () => {
  //设置加载标识符
  chatStore.setLoadingPrivateMsgState(true);
  console.log("max private messageId:", chatStore.privateMsgMaxId);
  //请求后端
  pullOfflinePrivateMessage(chatStore.privateMsgMaxId).catch((err) => {
    console.log("拉取私聊离线消息出错", err);
    chatStore.setLoadingPrivateMsgState(false);
  });
};

//TODO:拉取离线群聊消息
const pullGroupOfflineMsg = () => {
  chatStore.setLoadingGroupMsgState(true);
  console.log("max group messageId:", chatStore.groupMsgMaxId);
  pullOfflineGroupMessage(chatStore.groupMsgMaxId).catch((err) => {
    console.log("拉取群聊离线消息出错", err);
    chatStore.setLoadingGroupMsgState(false);
  });
};

//插入私聊消息(有人发消息)
const insertPrivateMsg = (friend, msgInfo) => {
  let chatInfo = {
    type: CHATINFO_TYPE.PRIVATE,
    targetId: friend.id,
    showName: friend.friendNickname,
    headImage: friend.headImage,
  };
  //打开会话
  chatStore.openChat(chatInfo);
  //插入信息
  chatStore.insertMessage(msgInfo, chatInfo);
  //TODO:私聊接收消息提示
};

//加载好友信息(好友给你发消息)
const loadFriendInfo = (friendId) => {
  let friend = friendStore.findFriend(friendId);
  if (!friend) {
    friend = {
      id: friendId,
      friendNickname: "未知用户",
      headImage: "",
    };
  }
  return friend;
};

//处理消息
const handlePrivateMessage = (msgInfo) => {
  // 标记这条消息是不是自己发的
  msgInfo.selfSend = msgInfo.sendId === userStore.userInfo.id;
  let friendId = msgInfo.selfSend ? msgInfo.recvId : msgInfo.sendId;

  //会话信息
  let chatInfo = {
    type: CHATINFO_TYPE.PRIVATE,
    targetId: friendId,
  };

  //加载消息
  if (msgInfo.type === MESSAGE_TYPE.LOADING) {
    console.log("私聊加载标志:", msgInfo.content);
    chatStore.setLoadingPrivateMsgState(JSON.parse(msgInfo.content));
    return;
  }

  // 已读消息
  if (msgInfo.type === MESSAGE_TYPE.READED) {
    chatStore.resetUnread(chatInfo);
    return;
  }

  // 消息回执处理,改消息状态为已读
  if (msgInfo.type === MESSAGE_TYPE.RECEIPT) {
    chatStore.readedMessage(friendId, null);
    return;
  }

  // 消息撤回
  if (msgInfo.type === MESSAGE_TYPE.RECALL) {
    chatStore.recallMsg(msgInfo, chatInfo);
    return;
  }

  // 新增好友
  if (msgInfo.type === MESSAGE_TYPE.FRIEND_NEW) {
    friendStore.addFriend(JSON.parse(msgInfo.content));
    return;
  }

  // (被)删除好友
  if (msgInfo.type === MESSAGE_TYPE.FRIEND_DEL) {
    friendStore.removeFriend(friendId);
    return;
  }

  //单人RTC信令
  if (checkMsgType.isRtcPrivate(msgInfo.type)) {
    rtcPrivateVideo.value.onRTCPrivateMsg(msgInfo);
    return;
  }

  //需要会话显示的消息
  if (
    checkMsgType.isNormal(msgInfo.type) ||
    checkMsgType.isTip(msgInfo.type) ||
    checkMsgType.isAction(msgInfo.type)
  ) {
    let friend = loadFriendInfo(friendId);
    insertPrivateMsg(friend, msgInfo);
  }
};

//TODO:处理群聊消息
const handleGroupMessage = (msgInfo) => {
  //表示是否是自己发的(其他终端或其他功能)
  msgInfo.selfSend = msgInfo.sendId === userStore.userInfo.id;
  let chatInfo = {
    type: CHATINFO_TYPE.GROUP,
    targetId: msgInfo.groupId,
  };
  //更改加载标记
  if (msgInfo.type === MESSAGE_TYPE.LOADING) {
    chatStore.setLoadingGroupMsgState(JSON.parse(msgInfo.content));
    return;
  }
  //收到已读信号，前端标记已读
  if (msgInfo.type === MESSAGE_TYPE.READED) {
    chatStore.resetUnread(chatInfo);
    return;
  }
  //回执消息
  if (msgInfo.type === MESSAGE_TYPE.RECEIPT) {
    // 更新消息已读人数
    let msgInfo_ = {
      id: msg.id,
      groupId: msg.groupId,
      readedCount: msg.readedCount,
      receiptOk: msg.receiptOk,
    };
    chatStore.updateMessage(msgInfo_, chatInfo);
    return;
  }
  //撤回消息信号
  if (msgInfo.type === MESSAGE_TYPE.RECALL) {
    chatStore.recallMsg(msgInfo, chatInfo);
    return;
  }
  //新增群聊
  if (msgInfo.type === MESSAGE_TYPE.GROUP_NEW) {
    groupStore.addGroup(JSON.parse(msgInfo.content));
    return;
  }
  // 删除群
  if (msgInfo.type === MESSAGE_TYPE.GROUP_DEL) {
    groupStore.removeGroup(JSON.parse(msgInfo.groupId));
    return;
  }
  //插入群聊消息
  if (
    checkMsgType.isNormal(msgInfo.type) ||
    checkMsgType.isTip(msgInfo.type) ||
    checkMsgType.isAction(msgInfo.type)
  ) {
    let group = loadGroupInfo(msgInfo.groupId);
    insertGroupMsg(group, msgInfo);
  }
  // TODO:群视频
};


const insertGroupMsg = (group, msgInfo) => {
  //每次创建一个chatInfo可以动态更新group的头像信息
  let chatInfo = {
    type: CHATINFO_TYPE.GROUP,
    targetId: group.id,
    showName: group.showGroupName,
    headImage: group.headImageThumb,
  };
  chatStore.openChat(chatInfo);
  chatStore.insertMessage(msgInfo, chatInfo);
};

//收到消息时可能前端没有创建会话，需要自行加载
const loadGroupInfo = (groupId) => {
  let group = groupStore.findGroup(groupId);
  if (!group) {
    group = {
      id: groupId,
      showGroupName: "未知群聊",
      headImageThumb: "",
    };
  }
  return group;
};

//TODO:处理系统消息
const handleSystemMessage = (msgInfo) => {};

//路由
const goTo = (path) => {
  router.push(path);
};

//初始化RTC面板监听器
const initRTC = () => {
  mitter.on("openPrivateVideoEvent", (rtcInfo) => {
    rtcPrivateVideo.value.open(rtcInfo);
  });
};

//卸载
const uninstallRTC = () => {
  mitter.off("openPrivateVideoEvent");
};

onMounted(() => {
  //初始化
  initRTC();
  init();
});

onUnmounted(() => {
  //关闭WebSocket
  wsApi.close();
  uninstallRTC();
});
</script>

<template>
  <el-container>
    <el-aside width="65px" class="navibar">
      <ul class="navilist">
        <li
          @click="goTo('/home/chat')"
          :class="{ active: route.path === '/home/chat' }"
        >
          <el-icon>
            <ChatDotRound />
          </el-icon>
        </li>
        <li
          @click="goTo('/home/friend')"
          :class="{ active: route.path === '/home/friend' }"
        >
          <el-icon>
            <User />
          </el-icon>
        </li>
        <li
          @click="goTo('/home/group')"
          :class="{ active: route.path === '/home/group' }"
        >
          <el-icon>
            <Grid />
          </el-icon>
        </li>
        <li
          @click="goTo('/home/setting')"
          :class="{ active: route.path === '/home/setting' }"
        >
          <el-icon>
            <Setting />
          </el-icon>
        </li>
      </ul>
    </el-aside>
    <el-main class="el-main">
      <div class="routerview-panel">
        <router-view></router-view>
      </div>
    </el-main>
    <RTCPrivateVideo ref="rtcPrivateVideo" />
  </el-container>
</template>

<style scoped>
.navibar {
  display: flex;
  justify-content: center;
  background-color: var(--theme-dark);
}

.navilist {
  height: 100vh;
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
    color: var(--theme-gray);

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

.el-main {
  padding: 0;
}

.routerview-panel {
  height: 100%;
  width: 100%;
}
</style>
