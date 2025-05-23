<script setup>
import {ChatDotRound, Grid, Setting, User} from "@element-plus/icons-vue";
import {onMounted} from "vue";
import useMainStore from "../store/index.js";

const mainStore = useMainStore()

const init = () => {
  mainStore.loadAll()
}

onMounted(() => {
  init()
})
</script>

<template>
  <el-container>
    <el-aside width="65px">
      <el-menu
          default-active="/home/chat"
          router
          class="menu"
      >
        <el-menu-item index="/home/chat">
            <el-icon> <ChatDotRound/> </el-icon>
        </el-menu-item>
        <el-menu-item index="/home/friend">
            <el-icon> <User/> </el-icon>
        </el-menu-item>
        <el-menu-item index="/home/group">
            <el-icon> <Grid/> </el-icon>
        </el-menu-item>
        <el-menu-item index="/home/setting">
            <el-icon> <Setting/> </el-icon>
        </el-menu-item>

      </el-menu>
    </el-aside>
    <el-main class="routerview">
      <div>
        <router-view></router-view>
      </div>
    </el-main>
  </el-container>
</template>

<style scoped>
.menu {
  height: 100vh;
}

.routerview{
  padding: 0;
}
</style>