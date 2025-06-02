<script setup>
//选择成员界面，用于多处
import {computed, ref} from "vue";
import useFriendStore from "../../store/friendStore.js";
import FriendItem from "../friend/FriendItem.vue";
import {inviteToGroup} from "../../api/group.js";
import {ElMessage} from "element-plus";

const friendStore = useFriendStore()

const props = defineProps({
  groupId: {
    type: Number,
  },
  members: {
    type: Array,
  },
  groupName: {
    type: String,
  }
})

const friends = ref([])
const show = ref(false)

//搜索文本
const open = () => {
  show.value = true
  friends.value = []
  friendStore.friends.forEach(friend => {
    if (friend.deleted) return
    let f = JSON.parse(JSON.stringify(friend))
    let m = props.members.filter(m => !m.quit).find(m => m.userId == f.id);
    //好友已经在群里
    if(m){
      f.checked = true;
      f.disabled = true;
    }else {
      f.checked = false;
      f.disabled = false;
    }
    friends.value.push(f)
  })
}

const close = () => {
  show.value = false
}

defineExpose({
  open,
  close
})

const dialogTitle = computed(() => {
  return "邀请好友到: " + props.groupName
})

//点击好友item选择加入
const onClickItem = (friend) => {
  if (!friend.disabled){
    friend.checked = !friend.checked
  }
}

const onSubmit = () => {
  let ids = []
  friends.value.forEach(f => {
    if (!f.disabled && f.checked) {
      ids.push(f.id)
    }
  })
  let inviteVO = {
    groupId: props.groupId,
    friendIds: ids
  }
  inviteToGroup(inviteVO).then(() => {
    close()
    ElMessage.success("邀请成功")
  })
}

</script>

<template>
  <el-dialog v-model="show" :modal="false" :title="dialogTitle" width="650">
    <div class="container">
      <div class="select-panel">
        <div class="select">
          <el-input placeholder="输入搜索"></el-input>
          <el-scrollbar>
            <div class="item-container">
              <div v-for="friend in friends" :key="friend.id">
                <friend-item :friend="friend" @click="onClickItem(friend)">
                  <el-checkbox :disabled="friend.disabled" v-model="friend.checked" @click.stop>
                  </el-checkbox>
                </friend-item>
              </div>
            </div>
          </el-scrollbar>
        </div>
        <div class="chosen">
          <el-scrollbar>
            <div class="item-container">
              <div v-for="friend in friends" :key="friend.id">
                <friend-item
                    v-if="friend.checked && !friend.disabled"
                    :friend="friend">
                </friend-item>
              </div>
            </div>
          </el-scrollbar>
        </div>
      </div>
      <el-button type="primary" @click="onSubmit">发送邀请</el-button>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">

.container{
  display: flex;
  flex-direction: column;
  align-items: center;
}

.select-panel {
  width: 100%;
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
}

.item-container {
  width: 97%;
}

.select {
  height: 100%;
  width: 300px;
  border: 1px solid #ccc;
  display: flex;
  flex-direction: column;
}

.chosen {
  height: 100%;
  width: 300px;
  border: 1px solid #ccc;
}
</style>