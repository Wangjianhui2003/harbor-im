<script setup>
import AddFriend from "../components/friend/AddFriend.vue";
import { computed, ref } from "vue";
import useFriendStore from "../store/friendStore.js";
import { pinyin } from "pinyin-pro";
import FriendItem from "../components/friend/FriendItem.vue";
import { getUserInfo } from "../api/user.js";
import useChatStore from "../store/chatStore.js";
import HeadImage from "../components/common/HeadImage.vue";
import { useRouter } from "vue-router";
import { CHATINFO_TYPE } from "../common/enums.js";
import { removeFriend } from "../api/friend.js";
import { ElMessage, ElMessageBox } from "element-plus";
import SearchBar from "../components/common/SearchBar.vue";

const router = useRouter();

const addFriendPanelVisible = ref(false);
//搜索栏文本

const searchText = ref("");
//要展示的好友用户信息

//正在查看的好友
const activeFriend = ref({});
//该好友的全量信息
const friendUserInfo = ref({});

const friendStore = useFriendStore();
const chatStore = useChatStore();

//将用户按照首字母分组
const friendMap = computed(() => {
  let map = new Map();
  friendStore.friends.forEach((f) => {
    //已删除好友和不在搜索结果的不展示
    if (
      f.deleted ||
      (searchText.value &&
        !f.friendNickname
          .toUpperCase()
          .includes(searchText.value.toString().toUpperCase()))
    ) {
      return;
    }
    let firstLetter = getFirstLetter(f.friendNickname).toUpperCase();
    // 非英文一律为#组
    if (!isEnglish(firstLetter)) {
      firstLetter = "#";
    }
    //
    if (f.online) {
      firstLetter = "在线";
    }
    if (map.has(firstLetter)) {
      map.get(firstLetter).push(f);
    } else {
      map.set(firstLetter, [f]);
    }
  });
  // 排序
  let arrayObj = Array.from(map);
  arrayObj.sort((a, b) => {
    // "在线"组在最前面
    if (a[0] == "在线") return -1;
    if (b[0] == "在线") return 1;
    // #组在最后面
    if (a[0] == "#") return 1;
    if (b[0] == "#") return -1;
    return a[0].localeCompare(b[0]);
  });
  map = new Map(arrayObj.map((i) => [i[0], i[1]]));
  return map;
});

//返回key(分组)
const friendKeys = computed(() => {
  return Array.from(friendMap.value.keys());
});

//返回value
const friendValues = computed(() => {
  return Array.from(friendMap.value.values());
});

//是否是friend
const isFriend = computed(() => {
  return friendStore.isFriend(friendUserInfo.value.id);
});

const showAddFriend = () => {
  addFriendPanelVisible.value = true;
};

const closeAddFriend = () => {
  addFriendPanelVisible.value = false;
};

//返回名字第一个字母
const getFirstLetter = (strText) => {
  // 使用pinyin-pro库将中文转换为拼音
  let pinyinOptions = {
    toneType: "none", // 无声调
    type: "normal", // 普通拼音
  };
  let pyText = pinyin(strText, pinyinOptions);
  return pyText[0];
};

//是否是英文
const isEnglish = (character) => {
  return /^[A-Za-z]+$/.test(character);
};

//点击好友标签触发
const onActiveItem = (friend) => {
  activeFriend.value = friend;
  loadUserInfo(friend.id);
};

//加载用户信息
const loadUserInfo = (userId) => {
  //api请求
  getUserInfo(userId).then((user) => {
    friendUserInfo.value = user;
    updateFriendInfo();
  });
};

//更新store好友信息
const updateFriendInfo = () => {
  if (isFriend) {
    const friend = {};
    friend.id = friendUserInfo.value.id;
    friend.headImage = friendUserInfo.value.headImageThumb;
    friend.friendNickname = friendUserInfo.value.nickname;
    friendStore.updateFriend(friend);
    chatStore.updateChatFromFriend(friend);
  }
};

/**
 * 点击发送消息按钮,传入要与之聊天的用户信息
 * @param user 要聊天的用户信息
 */
const onSendMsg = (user) => {
  let chat = {
    type: CHATINFO_TYPE.PRIVATE,
    targetId: user.id,
    showName: user.nickname,
    headImage: user.headImageThumb,
  };
  console.log("open chat:", chat);
  chatStore.openChat(chat);
  chatStore.activateChat(0);
  router.push({ name: "Chat" });
};

//删除好友(+缓存+聊天记录)
const onDelFriend = (userInfo) => {
  ElMessageBox.confirm(
    `确认删除'${userInfo.nickname}',并清空聊天记录吗?`,
    "删除好友",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    },
  ).then(() => {
    removeFriend(userInfo.id).then(() => {
      friendStore.removeFriend(userInfo.id);
      chatStore.removePrivateChat(userInfo.id);
      ElMessage.success("已删除好友");
      friendUserInfo.value = {};
      activeFriend.value = {};
    });
  });
};
</script>

<template>
  <el-container>
    <el-aside class="friend-list">
      <div class="friend-list-header">
        <search-bar
          class="search-bar"
          placeholder="搜索好友"
          v-model:search-text="searchText"
        ></search-bar>
        <el-button :round="true" class="add-button" @click="showAddFriend()"
          >+</el-button
        >
        <AddFriend
          :dialog-visible="addFriendPanelVisible"
          @close="closeAddFriend"
        />
      </div>
      <el-scrollbar class="friend-item-list">
        <div class="list-container">
          <div v-for="(friends, i) in friendValues" :key="i">
            <div>{{ friendKeys[i] }}</div>
            <div v-for="friend in friends" :key="friend.id">
              <FriendItem
                :friend="friend"
                @click="onActiveItem(friend)"
              ></FriendItem>
            </div>
          </div>
        </div>
      </el-scrollbar>
    </el-aside>
    <el-main>
      <div class="friend-info" v-show="friendUserInfo.id">
        <div class="info-header">
          <head-image
            class="avatar"
            :url="friendUserInfo.headImage"
            :name="friendUserInfo.nickname"
          >
          </head-image>
          <div class="info-text">
            <div class="username">用户名: {{ friendUserInfo.username }}</div>
            <div class="nickname">昵称: {{ friendUserInfo.nickname }}</div>
            <div class="remark">备注: {{ activeFriend.friendNickname }}</div>
          </div>
        </div>
        <div class="action-buttons">
          <el-button
            type="primary"
            class="send-message-btn"
            @click="onSendMsg(friendUserInfo)"
            >发送消息</el-button
          >
          <el-button
            type="danger"
            class="delete-friend-btn"
            @click="onDelFriend(friendUserInfo)"
            >删除好友</el-button
          >
        </div>
      </div>
    </el-main>
  </el-container>
</template>

<style scoped lang="scss">
.list-container {
  overflow: auto;
  width: 97%;
}

.friend-list {
  display: flex;
  flex-direction: column;
  width: 270px;
  height: 100vh;
  background-color: var(--theme-black);
  color: var(--theme-white);
  align-items: center;

  .friend-list-header {
    display: flex;
    margin: 10px;
    width: 90%;
    height: 40px;
    justify-content: space-between;
    align-items: center;

    .search-bar {
      height: 40px;
      width: 190px;
    }

    .add-button {
      height: 36px;
      width: 36px;
    }
  }

  .friend-item-list {
    width: 100%;
    flex: 1;
  }
}

.friend-info {
  .info-header {
    display: flex;
    align-items: center;
    margin-bottom: 20px;

    .avatar {
      width: 80px;
      height: 80px;
      box-shadow: 0 6px 8px rgba(0, 0, 0, 0.15);
      font-size: 30px;
    }
  }

  .action-buttons {
    display: flex;
    gap: 10px;

    .send-message-btn {
      background-color: #409eff;
      border-color: #409eff;
    }

    .delete-friend-btn {
      background-color: #f56c6c;
      border-color: #f56c6c;
    }
  }
}
</style>
