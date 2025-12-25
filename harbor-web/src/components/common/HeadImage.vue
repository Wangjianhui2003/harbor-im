<script setup>
import { computed, useAttrs } from "vue";

const props = defineProps({
  size: {
    type: Number, //头像大小
    default: 42,
  },
  url: {
    type: String, //头像url
    default: "",
  },
  name: {
    type: String, //名称（加载失败显示）
    default: "N",
  },
  online: {
    type: Boolean, //是否在线
    default: false,
  },
  id: {
    type: Number, //id
    default: -1,
  },
});

const errorAvatar = computed(() => {
  return props.name[0].toUpperCase();
});
</script>

<template>
  <!--  用户头像-->
  <div class="avatar">
    <el-avatar
      :class="{ online: props.online }"
      :src="props.url"
      fit="cover"
      :size="props.size"
    >
      {{ errorAvatar }}
    </el-avatar>
  </div>
</template>

<style scoped>
.online {
  border: 2px solid #4be740;
}
</style>
