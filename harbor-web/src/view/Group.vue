<script setup>

import {computed, reactive, ref} from "vue";
import AddGroupPanel from "../components/group/AddGroupPanel.vue";
import {ElMessage} from "element-plus";
import {useGroupStore} from "../store/groupStore.js";
import GroupItem from "../components/group/GroupItem.vue";
import {CHATINFO_TYPE} from "../common/enums.js";
import useChatStore from "../store/chatStore.js";
import {useRouter} from "vue-router";
import InvitePanel from "../components/group/InvitePanel.vue";
import {findGroupMembers} from "../api/group.js";
import SearchBar from "../components/common/SearchBar.vue";


const searchText = ref("");
const addPanelVisible = ref(false);
const activeGroupInfo = ref({})

//群成员
const groupMembers = ref([])
//
const showMaxIdx = ref(150)


const groupStore = useGroupStore();
const chatStore = useChatStore();

const router = useRouter();

const invitePanel = ref(null)

const onGroupCreated = () => {
  addPanelVisible.value = false;
  ElMessage.success("创建群聊成功")
}

const groupMap = computed(() => {
  let map = new Map()
  map.set("加入的群聊",[])
  groupStore.groups.forEach(group => {
    if(!isValidGroup(group)) {
      return
    }
    map.get("加入的群聊").push(group)
  })
  return map
})

const isValidGroup = (group) => {
  return group.quit != true && group.dissolve != true
  && group.name.toUpperCase().includes(searchText.value.toUpperCase())
  && group.showGroupName.toUpperCase().includes(searchText.value.toUpperCase())
}

const groupMapKeys = computed(() => {
  return Array.from(groupMap.value.keys());
})

const groupMapValues = computed(() => {
  return Array.from(groupMap.value.values());
})

const onActiveItem = (group) => {
  activeGroupInfo.value = group;
  reloadGroupMembers(group.id)
}

const reloadGroupMembers = (groupId) => {
  groupMembers.value = []
  findGroupMembers(groupId).then(groupMemberVOs => {
    console.log(groupMemberVOs)
    groupMembers.value = groupMemberVOs;
  })
}

const onClickSendButton = () => {
  let chat = {
    type: CHATINFO_TYPE.GROUP,
    showName: activeGroupInfo.value.showGroupName,
    targetId: activeGroupInfo.value.id,
    headImage: activeGroupInfo.value.image,
  }
  chatStore.openChat(chat)
  chatStore.activateChat(0)
  router.push('/home/chat')
}
</script>

<template>
  <div class="group-panel">
    <div class="group-list">
      <div class="list-header">
        <search-bar class="search-bar" placeholder="搜索群名" v-model:search-text="searchText" ></search-bar>
        <el-button class="add-button" @click="addPanelVisible = true">+</el-button>
        <add-group-panel
            :add-panel-visible="addPanelVisible"
            @close="addPanelVisible=false"
            @group-created="onGroupCreated" >
        </add-group-panel>
      </div>
      <el-scrollbar class="group-item-list">
        <div v-for="(groups,idx) in groupMapValues" :key="idx">
          {{groupMapKeys[idx]}}
          <div v-for="group in groups">
            <group-item
                :group="group"
                @click="onActiveItem(group)">
            </group-item>
          </div>
        </div>
      </el-scrollbar>
    </div>
    <div class="group-info-panel" v-show="activeGroupInfo.id">
      {{activeGroupInfo.name}}  ({{activeGroupInfo.showGroupName}})
      <el-button @click="onClickSendButton"> 发送信息 </el-button>
      <el-button @click="invitePanel.open()"> 邀请 </el-button>
    </div>
    <invite-panel
        :groupName="activeGroupInfo.showGroupName"
        :group-id="activeGroupInfo.id"
        :members="groupMembers"
        ref="invitePanel"
        @close="invitePanel.close()">
    </invite-panel>
  </div>
</template>

<style scoped lang="scss">
.group-panel{
  display: flex;
  height: 100%;
  width: 100%;

  .group-list{
    width: 270px;
    height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    overflow: hidden;
    background-color: var(--theme-black);
    color: var(--theme-white);

    .list-header{
      height: 40px;
      width: 90%;
      display: flex;
      margin: 10px;
      justify-content: space-between;
      align-items: center;
    }

    .search-bar{
      width: 190px;
      height: 40px;
    }

    .add-button{
      height: 36px;
      width: 36px;
      border-radius: 30px;
    }

    .group-item-list{
      width: 100%;
      flex: 1;
    }
  }

  .group-info-panel{
    flex: 1;
    background-color: #f5f7fa;
  }
}


</style>