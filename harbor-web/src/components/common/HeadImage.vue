<script setup>

import {computed} from "vue";

const props = defineProps({
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
    default: -1
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
      size="large"
      fit="cover"
      class="avatar"
  >
    {{errorAvatar}}
  </el-avatar>
</template>

<style scoped>
.avatar{

}
</style>