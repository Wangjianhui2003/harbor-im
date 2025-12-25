<script setup>
import HeadImage from "../common/HeadImage.vue";

const props = defineProps({
  group: {
    type: Object,
  },
});
</script>

<template>
  <div class="group-item">
    <head-image
      class="avatar"
      :url="props.group.headImageThumb"
      :name="props.group.showGroupName"
    >
    </head-image>
    <div class="group-info">
      {{ props.group.showGroupName }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.group-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px;
  margin-bottom: 4px;
  width: 260px;

  &:hover {
    background-color: var(--theme-gray);
  }

  .avatar {
    font-size: 20px;
    height: 50px;
    width: 50px;
  }

  .group-info {
    flex: 1;
    text-align: center;
  }
}
</style>
