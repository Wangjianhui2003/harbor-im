<script setup>
//文件上传

import {computed, ref} from "vue";
import {ElLoading, ElMessage} from "element-plus";
import uploadFile from "../../api/file.js";

const props = defineProps({
  //上传地址
  url: {
    type: String,
    required: false
  },
  fileTypes: {
    type: Array,
    default: null
  },
  maxSize: {
    type: Number,
    default: null
  },
  //是否显示加载动画
  showLoading: {
    type: Boolean,
    default: false
  },
  //禁用
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['uploadSuccess', 'uploadFail','before']);

//加载面板
const loading = ref(null)
// const uploadHeaders = {"accessToken": sessionStorage.getItem('accessToken')}

//字符串标识文件大小
const fileSizeStr = computed(() => {
  if (props.maxSize > 1024 * 1024) {
    return Math.round(props.maxSize / 1024 / 1024) + "M";
  }
  if (this.maxSize > 1024) {
    return Math.round(props.maxSize / 1024) + "KB";
  }
  return this.maxSize + "B";
})

//上传文件
const onFileUpload = (file) => {
  //需要加载动画
  if (props.showLoading) {
    loading.value = ElLoading.service({
      lock: true,
      text: "正在上传",
      spinner: "el-icon-loading",
      background: 'rgba(255,255,255,0.7)'
    })
  }
  let formData = new FormData();
  formData.append("file", file.file)
  //调用后端接口上传
  uploadFile(formData, props.url, {'Content-Type': 'multipart/form-data'})
  .then((data) => {
    emit("uploadSuccess", data)
  }).catch((err) => {
    emit("uploadFail", err, file.file)
  }).finally(() => {
    //关闭加载
    loading.value && loading.value.close();
  })
}

//文件上传前校验
const beforeUpload = (file) => {
  //校验类型
  if(props.fileTypes && props.fileTypes.length > 0){
    let fileType = file.type
    let t = props.fileTypes.find((t) => t.toLowerCase() === fileType)
    if(t === undefined){
      ElMessage.error(`文件格式错误，请上传以下格式文件：${props.fileTypes.join("、")}`);
      //返回false停止上传
      return false;
    }
  }
  //校验大小
  if(props.maxSize && file.size > props.maxSize){
    ElMessage.error(`文件大小不能超过 ${fileSizeStr}!`);
    return false;
  }
  // emit("before",file);
  return true;
}

</script>

<template>
  <el-upload :action="'#'"
             :http-request="onFileUpload"
             :accept="props.fileTypes == null ? '' : props.fileTypes.join(',')"
             :show-file-list="false"
             :disabled="props.disabled"
             :before-upload="beforeUpload"
             :multiple="true">
    <slot></slot>
  </el-upload>
</template>

<style scoped>

</style>