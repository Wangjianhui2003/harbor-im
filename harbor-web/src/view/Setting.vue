<script setup>
import FileUpload from "../components/common/FileUpload.vue";
import { onMounted, onUpdated, reactive, ref } from "vue";
import { Upload, UploadFilled } from "@element-plus/icons-vue";
import { updateUserInfo } from "../api/user.js";
import useUserStore from "../store/userStore.js";
import { ElMessage } from "element-plus";

/**
 * 设置页面
 */

//上传文件操作
const uploadImage = "/image/upload";
//5MB
const maxSize = 5 * 1024 * 1024;
//文件类型
const fileTypes = ["image/jpeg", "image/png", "image/jpg", "image/webp"];

const userInfo = reactive({});
const settingForm = reactive(null);

const userStore = useUserStore();

//头像上传成功后更新
const onUploadSuccess = (data) => {
  userInfo.headImage = data.originUrl;
  userInfo.headImageThumb = data.thumbUrl;
};

//提交表单
const onSubmit = () => {
  if (!validForm()) {
  }
  updateUserInfo(userInfo).then((data) => {
    //写回store
    userStore.setUserInfo(userInfo);
    ElMessage.success("修改成功");
  });
};

const validForm = () => {
  return true;
};

//挂载后加载用户信息
onMounted(() => {
  Object.assign(userInfo, userStore.userInfo);
});
</script>

<template>
  <div class="setting-panel"></div>
  <el-form :model="userInfo">
    <el-form-item label="头像">
      <file-upload
        class="avatar-uploader"
        :url="uploadImage"
        :show-loading="true"
        :max-size="maxSize"
        :file-types="fileTypes"
        ref="settingForm"
        @uploadSuccess="onUploadSuccess"
      >
        <img
          v-if="userInfo.headImage"
          class="uploaded-avatar"
          :src="userInfo.headImage"
          alt="headImage"
        />
        <el-icon v-else class="upload-icon" size="100">
          <UploadFilled />
        </el-icon>
      </file-upload>
    </el-form-item>
    <el-form-item label="用户名">
      <el-input
        disabled
        v-model="userInfo.username"
        autocomplete="off"
        size="small"
      ></el-input>
    </el-form-item>
    <el-form-item prop="nickname" label="昵称">
      <el-input
        v-model="userInfo.nickname"
        autocomplete="off"
        size="small"
      ></el-input>
    </el-form-item>
    <el-form-item label="性别">
      <el-radio-group v-model="userInfo.sex">
        <el-radio :label="0">男</el-radio>
        <el-radio :label="1">女</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="个性签名">
      <el-input
        type="textarea"
        v-model="userInfo.signature"
        :rows="3"
        maxlength="64"
      ></el-input>
    </el-form-item>
  </el-form>
  <el-button @click="onSubmit"> 确认 </el-button>
</template>

<style scoped lang="scss">
.uploaded-avatar {
  width: 120px;
}
</style>
