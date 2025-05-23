<script setup>
import AddFriend from "../components/friend/AddFriend.vue"
import {computed, ref} from "vue"
import {Plus} from "@element-plus/icons-vue"
import useFriendStore from "../store/friendStore.js";
import {pinyin} from "pinyin-pro";
import FriendItem from "../components/friend/FriendItem.vue";
import {getUserInfo} from "../api/user.js";
import useChatStore from "../store/chatStore.js";
import {useRouter} from "vue-router";

const addFriendPanelVisible = ref(false)
const router = useRouter()

//搜索栏文本
const searchText = ref("")

//要展示的好友用户信息
const activeFriend = ref({})
const friendUserInfo = ref({})

const friendStore = useFriendStore()
const chatStore = useChatStore()

//将用户按照首字母分组
const friendMap = computed(() => {
  let map = new Map()
  friendStore.friends.forEach((f) => {
    //已删除好友和不在搜索结果的不展示
    if(f.deleted || (searchText.value && !f.friendNickname.toUpperCase().includes(searchText.value.toString().toUpperCase()))){
      return;
    }
    let firstLetter = getFirstLetter(f.friendNickname).toUpperCase();
    // 非英文一律为#组
    if (!isEnglish(firstLetter)) {
      firstLetter = "#"
    }
    //
    if (f.online) {
      firstLetter = '在线'
    }
    if (map.has(firstLetter)) {
      map.get(firstLetter).push(f);
    } else {
      map.set(firstLetter, [f]);
    }
  })
  // 排序
  let arrayObj = Array.from(map);
  arrayObj.sort((a, b) => {
    // #组在最后面
    if (a[0] == '#' || b[0] == '#') {
      return b[0].localeCompare(a[0])
    }
    return a[0].localeCompare(b[0])
  })
  map = new Map(arrayObj.map(i => [i[0], i[1]]));
  return map;
})

// Show the add friend panel
const showAddFriend = () => {
  addFriendPanelVisible.value = true;

};
// Close the add friend panel
const closeAddFriend = () => {
  addFriendPanelVisible.value = false;

};

//返回名字第一个字母
const getFirstLetter = (strText) => {
  // 使用pinyin-pro库将中文转换为拼音
  let pinyinOptions = {
    toneType: 'none', // 无声调
    type: 'normal' // 普通拼音
  };
  let pyText = pinyin(strText, pinyinOptions);
  return pyText[0];
}

//是否是英文
const isEnglish = (character) => {
  return /^[A-Za-z]+$/.test(character);
}

//返回key(分组)
const friendKeys = computed(() => {
  return Array.from(friendMap.value.keys());
})

//返回value
const friendValues = computed(() => {
  return Array.from(friendMap.value.values());
})

//点击好友标签触发
const onActiveItem = (friend) => {
  loadUserInfo(friend.id)
  activeFriend.value = friend;
}

//加载用户信息
const loadUserInfo = (userId) => {
  //api请求
  getUserInfo(userId).then((user) => {
    friendUserInfo.value = user;
    updateFriendStore()
  })
}

//更新store好友信息
const updateFriendStore = () => {
  const friend = {}
  friend.id = friendUserInfo.value.id
  friend.headImage = friendUserInfo.value.headImageThumb
  friend.friendNickname = friendUserInfo.value.nickname
  friendStore.updateFriend(friend)
}

/**
 * 点击发送消息按钮,传入要与之聊天的用户信息
 * @param user 要聊天的用户信息
 */
const onSendMsg = (user) => {
  let chat = {
    type: 'PRIVATE',
    targetId: user.id,
    showName: user.nickname,
    headImage: user.headImageThumb,
  }
  console.log("chat:", chat)
  chatStore.openChat(chat)
  chatStore.activeChat(0)
  router.push("/home/chat")
}
</script>

<template>
  <el-container>
    <el-aside class="friend-list">
      <div>
        <el-input type="text" v-model="searchText" class="friend-search"></el-input>
        <el-button @click="showAddFriend()">
          <el-icon> <Plus/> </el-icon>
        </el-button>
        <AddFriend :dialog-visible="addFriendPanelVisible" @close="closeAddFriend"/>
      </div>
      <el-scrollbar class="friend-item-list">
        <div v-for="(friends, i) in friendValues" :key="i">
          <div>{{ friendKeys[i]}}</div>
          <div v-for="friend in friends" :key="friend.id">
            <FriendItem :friend="friend" @click="onActiveItem(friend)"></FriendItem>
          </div>
        </div>
      </el-scrollbar>
    </el-aside>
    <el-main>
      <div class="friend-info" v-show="friendUserInfo.id">
        <div class="info-header">
          <img :src="friendUserInfo.headImage" alt="头像" class="avatar"/>
          <div class="info-text">
            <div class="username">用户名: {{friendUserInfo.username}}</div>
            <div class="nickname">昵称: {{friendUserInfo.nickname}}</div>
            <div class="remark">备注: {{activeFriend.friendNickname}}</div>
          </div>
        </div>
        <div class="action-buttons">
          <el-button type="primary" class="send-message-btn" @click="onSendMsg(friendUserInfo)">发送消息</el-button>
          <el-button type="danger" class="delete-friend-btn">删除好友</el-button>
        </div>
      </div>
    </el-main>
  </el-container>
</template>

<style scoped lang="scss">
.friend-list {
  display: flex;
  flex-direction: column;
  width: 270px;
  height: 100vh;

  .friend-search {
    width: 200px;
    height: 30px;
  }

  .friend-item-list {
    flex: 1;
  }
}

.friend-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin: 20px;

  .info-header {
    display: flex;
    align-items: center;
    margin-bottom: 20px;

    .avatar {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      margin-right: 15px;
      object-fit: cover;
      border: 2px solid #fff;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .info-text {
      display: flex;
      flex-direction: column;

      .username, .nickname, .remark {
        font-size: 16px;
        margin: 2px 0;
        color: #333;
      }
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