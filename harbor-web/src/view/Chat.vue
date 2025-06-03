<script setup>
import ChatBox from "../components/chat/ChatBox.vue";
import useChatStore from "../store/chatStore.js";
import ChatItem from "../components/chat/ChatItem.vue";
import {ref} from "vue";
import SearchBar from "../components/common/SearchBar.vue";
//聊天界面

const chatStore = useChatStore();
const loading = chatStore.isLoading

//搜索文本
const searchText = ref('')

//选择一个聊天
const onActiveItem = (idx) => {
  chatStore.activateChat(idx)
}

//删除
const onDeleteItem = (idx) => {
  chatStore.removeChat(idx)
}

//移动到顶部
const onTop = (idx) => {
  chatStore.moveTop(idx)
}

</script>

<template>
  <el-container>
    <el-aside class="chat-list">
      <div class="list-header">
        <search-bar class="search-bar" v-model:search-text="searchText" placeholder="查找聊天..." ></search-bar>
      </div>
      <el-scrollbar>
        <div v-for="(chat,idx) in chatStore.chats" :key="idx">
          <chat-item
              v-show="!chat.delete && chat.showName && chat.showName.includes(searchText)"
              :chat="chat"
              :index="idx"
              :active="chat === chatStore.activeChat"
              @click="onActiveItem(idx)"
              @delete="onDeleteItem(idx)"
              @top="onTop(idx)"
          >
          </chat-item>
        </div>
      </el-scrollbar>
    </el-aside>
    <el-main class="chat-box">
      <chat-box v-if="chatStore.activeChat"
                :chat="chatStore.activeChat">
      </chat-box>
    </el-main>
  </el-container>
</template>

<style scoped lang="scss">
.list-header{
  margin: 10px;
}

.search-bar{
  height: 40px;
  width: 100%;
}

.chat-list {
  display: flex;
  flex-direction: column;
  width: 270px;
  height: 100vh;
  background-color: var(--theme-black);
}

.input {
  background-color: #f5f5f5;
  color: #242424;
  padding: .15rem .5rem;
  min-height: 40px;
  border-radius: 4px;
  outline: none;
  border: none;
  line-height: 1.15;
  box-shadow: 0px 10px 20px -18px;

  &:focus {
    border-bottom: 2px solid #5b5fc7;
    border-radius: 4px 4px 2px 2px;
  }

  &:hover {
    outline: 1px solid lightgrey;
  }
}

.chat-box {
  padding: 0px;
}

</style>