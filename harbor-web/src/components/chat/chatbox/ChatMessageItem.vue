<script setup>

const props = defineProps({
  msgInfo: {
    type: Object
  },
  mine:{
    type: Boolean
  },
  headImage: {
    type: String,
  },
})
</script>

<template>
  <div class="">
    {{props.msgInfo.content}}
  </div>
</template>

<style scoped lang="scss">

</style>