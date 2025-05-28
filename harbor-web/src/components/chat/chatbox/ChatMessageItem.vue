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
    required: true
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