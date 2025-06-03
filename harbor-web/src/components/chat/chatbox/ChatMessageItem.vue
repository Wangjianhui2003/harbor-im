<script setup>

import {computed, ref} from "vue";
import {MESSAGE_TYPE, MSG_ITEM_OP} from "../../../common/enums.js";
import * as dateUtil from "../../../common/date.js";
import * as checkMsgType from "../../../common/checkMsgType.js";
import HeadImage from "../../common/HeadImage.vue";

const props = defineProps({
  msgInfo: {
    type: Object  //消息
  },
  mine: {
    type: Boolean //自己的消息
  },
  headImage: {
    type: String,
  },
  showName: {
    type: String //显示名称
  },
  groupMembers: {
    type: Array //群成员
  },
  haveRightMenu: {
    type: Boolean, //是否右键有菜单
    default: true
  }
})

const audioPlayState = ref('STOP')

//消息在加载
const loading = computed(() => {
  return props.msgInfo.loadStatus && props.msgInfo.loadStatus === 'loading'
})

//右键菜单栏项目
const menuItems = computed(() => {
  let items = []
  items.push({
    key: MSG_ITEM_OP.DELETE,
    name: '删除'
  })
  if (props.msgInfo.selfSend && props.msgInfo.id > 0) {
    items.push({
      key: MSG_ITEM_OP.RECALL,
      name: '撤回',
    })
  }
  return items
})

const isNormal = () => {
  return checkMsgType.isNormal(props.msgInfo.type) || checkMsgType.isAction(props.msgInfo.type)
}

const selfSend = computed(() => {
  return props.msgInfo.selfSend
})
</script>

<template>
  <div class="msg-item">
    <div v-if="props.msgInfo.type === MESSAGE_TYPE.TIP_TEXT" class="tip">
      {{ props.msgInfo.content }}
    </div>
    <div v-else-if="props.msgInfo.type === MESSAGE_TYPE.TIP_TIME" class="time-tip">
      {{ dateUtil.toTimeText(props.msgInfo.sendTime) }}
    </div>
    <div v-else-if="isNormal" class="normal" :class="{selfSendRow : selfSend}">
      <div>
        <head-image
            :size="36"
            :url="props.headImage"
            :name="props.showName"
            :id="props.msgInfo.sendId" >
        </head-image>
      </div>
      <div class="content-container" :class="{selfSendContent : selfSend}">
        <div class="send-info" :class="{selfSendRow : selfSend}">
          <div>
            {{props.showName}}
          </div >
          <div class="send-time">
            {{dateUtil.toTimeText(props.msgInfo.sendTime)}}
          </div>
        </div>
        <div class="content">
          {{ props.msgInfo.content}}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">

.content{
  background-color: #94C2ED;
  padding: 10px;
  border-radius: 9px;
}

.tip{
  margin: 45px 0;
  text-align: center;
  color: gray;
  font-size: 12px;
}

.time-tip {
  color: #23c483;
}

.msg-item{
  margin-top: 18px;
}

.send-info{
  height: 20px;
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.send-time{
  color: gray;
  font-size: 12px;
  margin: 0 20px
}

.normal {
  margin: 10px;
  display: flex;
}

.selfSendRow{
  flex-direction: row-reverse;
}

.content-container{
  display: flex;
  flex-direction: column;
  margin: 0 10px;
  max-width: 30%;
}

.selfSendContent{
  align-items: flex-end;
}

.send-info{
  display: flex;
}


</style>