<script setup>
/**
 * 群成员头像+名字
 */
import HeadImage from "../common/HeadImage.vue";

const props = defineProps({
  member:{
    type: Object,
    required: true
  }
})

</script>

<template>
  <div>
    <head-image
        :url="props.member.headImage"
        :name="props.member.showNickname"
        :online="props.member.online" >
    </head-image>
  </div>

</template>

<style scoped lang="scss">

</style>