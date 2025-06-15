<script setup>

import HeadImage from "../common/HeadImage.vue";
/**
 * 呼叫实时通话时，接听方的面板
 */
const props = defineProps({
  friend: {
    type: Object,
  },
  modeText: {
    type: String,
    required: true,
  }
})

const emit = defineEmits(['acceptRTCPrivateEvent','rejectRTCPrivateEvent']);

</script>

<template>
  <div class="call-panel">
    <head-image
        :url="props.friend.headImage"
        :name="props.friend.friendNickname"
        :id="props.friend.id"
        :size="70"
    >
    </head-image>
    <div class="information">
      {{props.friend.friendNickname}}向你请求{{props.modeText}}通话
      <div class="button-group">
        <button class="accept button" @click="emit('acceptRTCPrivateEvent')">接受</button>
        <button class="refuse button" @click="emit('rejectRTCPrivateEvent')">拒绝</button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">

.button-group {
  margin-top: 10px;
}

.information{
  margin-left: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.call-panel {
  min-width: 300px;
  max-width: 100hv;
  height: 100px;
  position: absolute;
  right: 10px;
  top: 10px;
  background-color: white;

  display: flex;
  align-items: center;

  padding: 1rem;
  border: 1px solid rgba(0, 0, 0, 0.45);
  border-radius: 10px;
  box-shadow: 20px 20px 30px rgba(0, 0, 0, .05);

  animation: animated-border 1.5s infinite;
}

@keyframes animated-border {
  0% {
    box-shadow: 0 0 0 0 rgba(115, 115, 115, 0.49);
  }
  100% {
    box-shadow: 0 0 0 20px rgba(255, 140, 140, 0);
  }
}

.button{
  font-size: 0.75rem;
  line-height: 1rem;
  font-weight: 500;
  border-radius: 0.5rem;
  padding-left: 1rem;
  padding-right: 1rem;
  padding-top: 0.625rem;
  padding-bottom: 0.625rem;
  border: none;
  transition: all .15s cubic-bezier(0.4, 0, 0.2, 1);
  margin: 0px 10px
}

.accept{
  background-color: rgb(17 24 39);
  color: #fff;

  &:hover {
    background-color: rgb(55 65 81);
  }
}

.refuse{
  background-color: rgb(225, 225, 225);
  color: #000000;

  &:hover {
    background-color: rgb(208, 208, 208);
  }
}


</style>