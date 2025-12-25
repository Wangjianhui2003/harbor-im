<script setup>
//聊天会话标签

import HeadImage from "../common/HeadImage.vue";
import { computed } from "vue";

const props = defineProps({
  chat: {
    type: Object,
  },
  active: {
    type: Boolean,
  },
});

//最后一条消息
const lastContent = computed(() => {
  let str = props.chat.lastContent;
  if (str.length > 14) {
    return str.substring(0, 14) + "...";
  }
  return str;
});
</script>

<template>
  <div class="chat-item">
    <head-image
      class="avatar"
      :url="props.chat.headImage"
      :name="props.chat.showName"
      :id="props.chat.targetId"
    >
    </head-image>
    <div class="chat-mini-info">
      <div class="name">{{ props.chat.showName }}</div>
      <div class="last-content">{{ lastContent }}</div>
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
}

.chat-mini-info {
  flex: 1;
  text-align: center;

  display: flex;
  flex-direction: column;
  align-items: flex-start;

  margin-left: 10px;
}

.last-content {
  color: #a6a6a6;
  font-size: 12px;
  margin-top: 2px;
}

.name {
  font-size: 14px;
}
</style>
