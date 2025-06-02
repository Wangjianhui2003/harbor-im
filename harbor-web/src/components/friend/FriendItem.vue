<script setup>
import HeadImage from "../common/HeadImage.vue";
import {computed} from "vue";

const props = defineProps({
  active: {
    type: Boolean,
  },
  friend: {
    type: Object,
  },
  menu: {
    type: Boolean,
    default: true
  },
});

</script>

<template>
  <div class="friend-item">
    <head-image
        :url="props.friend.headImage"
        :name="props.friend.friendNickname"
        :online="props.friend.online" >
    </head-image>
    <div class="friend-info">
      {{ props.friend.friendNickname }}
    </div>
    <slot></slot>
  </div>
</template>

<style lang="scss" scoped>

.friend-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 3px;
  margin: 4px;
  transition: transform 0.2s, box-shadow 0.2s;
  border-radius: 10px;

  &:hover {
    background-color: var(--theme-gray);
  }

  .friend-info {
    flex: 1;
    text-align: center;
  }
}

</style>