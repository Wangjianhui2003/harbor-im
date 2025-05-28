<script setup>

import {ref} from "vue";
import {Search} from "@element-plus/icons-vue";
import {findUserByName} from "../../api/user.js";
import useUserStore from "../../store/userStore.js";
import HeadImage from "../common/HeadImage.vue";
import {ElMessage} from "element-plus";
import {addFriend} from "../../api/friend.js";
import useFriendStore from "../../store/friendStore.js";

const props = defineProps({
  dialogVisible: {
    type: Boolean,
    default: false
  }
})

//用户列表
const users = ref([])

//搜索文本
const searchText = ref("")

//查询用户
const onSearch = () => {
  if (!searchText.value) {
    return;
  }
  isLoading.value = true;
  findUserByName(searchText.value).then((data) => {
    users.value = data;
    isLoading.value = false;
  })
}

//pannal loading
const isLoading = ref(false)

const userStore = useUserStore()
const friendStore = useFriendStore()

const isFriend = (userId) => {
  return friendStore.isFriend(userId)
}

const onAddFriend = (user) => {
  addFriend(user.id).then(() => {
    ElMessage.success("添加好友成功")
    let friendInfo = {
      id: user.id,
      friendNickname: user.nickname,
      headImage: user.headImage,
      online: user.online
    }
    friendStore.addFriend(friendInfo)
  })
}

</script>

<template>
  <!--  添加好友面板-->
  <el-dialog
      title="添加好友"
      class="dialog"
      v-model="props.dialogVisible"
      width="500px"
      v-loading="isLoading"
  >
    <el-input type="text" placeholder="输入用户名或昵称来查找" v-model="searchText" @keyup.enter="onSearch()">
      <template #suffix>
        <el-icon>
          <Search/>
        </el-icon>
      </template>
    </el-input>
    <el-scrollbar class="userinfo-list">
      <!--      不能用v-if,user.id还没出来就开始判断了-->
      <div v-for="(user) in users" :key="user.id" v-show="userStore.userInfo.id !== user.id">
        <div class="item">
          <head-image>{{ user.username }}</head-image>
          <div class="item-info"> {{ user.username }} {{ user.nickname }} {{ user.online }}</div>
          <el-button v-show="!isFriend(user.id)" class="item-button" @click="onAddFriend(user)">
            添加好友
          </el-button>
          <el-button v-show="isFriend(user.id)" class="item-button" disabled>已添加</el-button>
        </div>
      </div>
    </el-scrollbar>
  </el-dialog>
</template>

<style scoped lang="scss">
.userinfo-list {
  height: 400px;

  .item {
    display: flex;
    align-items: center;
    padding: 10px;
    transition: background-color 0.3s;

    &:hover {
      background-color: #f5f7fa;
    }

    .item-info {
      flex: 1;
      margin-left: 10px;
      font-size: 14px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .item-button {
      margin-left: 10px;
    }
  }
}
</style>