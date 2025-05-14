<script setup>

</script>

<template>
  <el-button type="primary">Primary</el-button>
  <el-scrollbar height="400px">
    <p v-for="item in 20" :key="item" class="scrollbar-demo-item">{{ item }}</p>
  </el-scrollbar>
</template>

<style scoped>

</style>
