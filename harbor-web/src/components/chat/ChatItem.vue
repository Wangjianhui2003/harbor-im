<script setup>
//聊天会话标签

import HeadImage from "../common/HeadImage.vue";

const props = defineProps({
  chat: {
    type: Object
  },
  active: {
    type: Boolean
  },
})

</script>

<template>
  <div class="chat-item">
    <head-image class="avatar"
                :url="props.chat.headImage"
                :name="props.chat.showName"
                :id="props.chat.targetId">
    </head-image>
    <div class="chat-mini-info">
      <div>{{ props.chat.showName }}</div>
    </div>
  </div>
</template>

<style scoped lang="scss">

.chat-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px;
  transition: transform 0.2s;
  margin-bottom: 4px;
  width: 260px;
  color: var(--theme-white);

  &:hover {
    background-color: var(--theme-gray);
  }

  .avatar {
    font-size: 20px;
    height: 50px;
    width: 50px;
  }

  .chat-mini-info {
    flex: 1;
    text-align: center;
  }
}
</style>