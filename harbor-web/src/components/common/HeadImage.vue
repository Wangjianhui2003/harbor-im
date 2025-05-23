<script setup>

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
    default: ''
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

</script>

<template>
<!--  用户头像-->
  <el-avatar :src="props.url" :size="props.size" fit="cover">
    <slot></slot>
  </el-avatar>
</template>

<style scoped>

</style>