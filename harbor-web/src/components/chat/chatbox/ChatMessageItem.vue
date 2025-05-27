<script setup>

const props = defineProps({
  msgInfo: {
    type: Object
  },
  mine:{
    type: Boolean
  }
})
</script>

<template>
  <div>
    {{props.msgInfo.content}}
  </div>
</template>

<style scoped lang="scss">

</style>