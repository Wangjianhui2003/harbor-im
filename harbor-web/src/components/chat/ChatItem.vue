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
  index: {
    type: Number
  }
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
  padding: 8px;
  transition: transform 0.2s, box-shadow 0.2s;
  margin: 2px;

  &:hover {
    transform: translateY(-5px);
    box-shadow: rgba(0, 0, 0, 0.25) 0px 14px 28px, rgba(0, 0, 0, 0.22) 0px 10px 10px;
  }

  .avatar {
    box-shadow: 0 6px 8px rgba(0, 0, 0, 0.15);
    font-size: 20px;
  }

  .chat-mini-info {
    flex: 1;
    text-align: center;
  }
}
</style>