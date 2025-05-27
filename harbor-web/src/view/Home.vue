<script setup>
import {ChatDotRound, User} from "@element-plus/icons-vue";
import {onMounted} from "vue";
import useMainStore from "../store/index.js";
import {useRoute, useRouter} from "vue-router";
/**
 * 主页
 */

const router = useRouter()
const route = useRoute()
const mainStore = useMainStore()

//加载store
const init = () => {
  mainStore.loadAll().then(() => {
  })
}

//路由
const goTo = (path) => {
  router.push(path)
}

onMounted(() => {
  init()
})
</script>

<template>
  <el-container>
    <el-aside width="65px" class="navibar">
      <ul class="navilist" >
        <li @click="goTo('/home/chat')" :class="{ active: route.path === '/home/chat'}">
          <el-icon> <ChatDotRound/> </el-icon>
        </li>
        <li @click="goTo('/home/friend')" :class="{ active: route.path === '/home/friend'}">
          <el-icon > <User/> </el-icon>
        </li>
        <li @click="goTo('/home/group')" :class="{ active: route.path === '/home/group'}">
          <el-icon > <Grid/> </el-icon>
        </li>
        <li @click="goTo('/home/setting')" :class="{ active: route.path === '/home/setting'}">
          <el-icon > <Setting/> </el-icon>
        </li>
      </ul>
    </el-aside>
    <el-main class="routerview">
      <div>
        <router-view></router-view>
      </div>
    </el-main>
  </el-container>
</template>

<style scoped>

.navibar {
  display: flex;
  justify-content: center;
  border-right: #dcdfe6 solid 1px;
}

.navilist {
  height: 100vh;
  background: #ffffff;
  text-align: center;
  padding-top: 10px;

  li {
    height: 45px;
    width: 45px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 9px;
    border-radius: 10px;

    .el-icon{
      font-size: 30px;
    }

    &:hover{
      background-color: #23d990;
      box-shadow: 0px 15px 20px rgba(46, 229, 157, 0.4);
      color: #fff;
      transition: all 0.3s ease 0s;
    }

    &.active{
      background-color: #23c483;
      box-shadow: 0px 15px 20px rgba(46, 229, 157, 0.4);
      color: #fff;
    }
  }
}

.routerview {
  padding: 0;
}
</style>