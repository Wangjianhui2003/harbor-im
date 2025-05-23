<script setup>
import HeadImage from "../common/HeadImage.vue";

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
    <div class="friend-avatar">
      <head-image :size="42" ></head-image>
    </div>
    <div class="friend-info">
      {{props.friend.friendNickname}}
    </div>
  </div>
</template>

<style lang="scss" scoped>

.friend-item{
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 10px;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 6px 8px rgba(0, 0, 0, 0.15);
  }

  .friend-info{
    flex: 1;
    text-align: center;
  }
}

</style>