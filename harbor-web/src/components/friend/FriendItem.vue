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

const errorHeadImage = computed(() => {
  return props.friend.friendNickname[0].toUpperCase();
})

</script>

<template>
  <div class="friend-item">
    <head-image
        class="avatar"
        :url="friend.headImage"
        :name="props.friend.friendNickname"
    >
    </head-image>
    <div class="friend-info">
      {{ props.friend.friendNickname }}
    </div>
  </div>
</template>

<style lang="scss" scoped>

.friend-item {
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

  .friend-info {
    flex: 1;
    text-align: center;
  }
}

</style>