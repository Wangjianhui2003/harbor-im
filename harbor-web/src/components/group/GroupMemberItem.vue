<script setup>
/**
 * 群聊用户详细item
 */

import HeadImage from "../common/HeadImage.vue";

const props = defineProps({
  member: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <div class="member-item">
    <head-image
      :id="props.member.userId"
      :online="props.member.online"
      :name="props.member.showNickname"
      :url="props.member.headImage"
    >
    </head-image>
    <div>{{ props.member.showNickname }}</div>
  </div>
</template>

<style scoped lang="scss">
.member-item {
  border: 1px solid black;
  display: flex;
  align-items: center;
}
</style>
