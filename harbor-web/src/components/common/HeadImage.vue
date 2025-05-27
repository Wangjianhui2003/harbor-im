<script setup>

import {computed} from "vue";

const props = defineProps({
  size: {
    type: Number,
    default: 50
  },
  url: {
    type: String,
    default: ''
  },
  name: {
    type: String,
    default: 'N'
  },
  online: {
    type: Boolean,
    default: false
  },
  id: {
    type: Number,
    default: ''
  }
})

const errorAvatar = computed(() => {
  return props.name[0].toUpperCase()
})

</script>

<template>
<!--  用户头像-->
  <el-avatar
      :src="props.url"
      :size="props.size"
      fit="cover"
      class="avatar"
  >
    {{errorAvatar}}
  </el-avatar>
</template>

<style scoped>
</style>