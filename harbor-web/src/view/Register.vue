<script setup>

import {ElMessage} from "element-plus";
import {reactive, ref, onMounted} from "vue";
import {register, getCaptcha} from "../api/user.js";
import {useRouter} from "vue-router";

const router = useRouter();

//登录表单组件
const formRef = ref();

//表单数据
const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  confirmPassword: '',
  captcha: '',
  captchaKey: ''
})

// 验证码图片
const captchaImage = ref('');

// 获取验证码
const fetchCaptcha = async () => {
  try {
    const data = await getCaptcha();
    if (data && data.captchaPic && data.captchaKey) {
      captchaImage.value = data.captchaPic;
      registerForm.captchaKey = data.captchaKey;
    } else {
      ElMessage.error('获取验证码失败');
    }
  } catch (error) {
    console.error('获取验证码出错:', error);
    ElMessage.error('获取验证码失败');
  }
}

// 页面加载时获取验证码
onMounted(() => {
  fetchCaptcha();
});

//提交表单
const submitForm = () => {
  if (!formRef.value) return
  formRef.value.validate(valid => {
    if (valid) {
      register(registerForm).then((data) => {
          ElMessage.success("注册成功,跳转登录页面")
          router.push("/login")
      })
    }else {
      ElMessage.error("请检查输入")
    }
  })
}

//重置表单
const resetForm = () => {
  formRef.value.resetFields();
  fetchCaptcha(); // 重置表单时刷新验证码
}

//el表单校验规则
const rules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 3, max: 20, message: '长度在 3 到 20 个字符之间', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, max: 20, message: '长度在 6 到 20 个字符之间', trigger: 'blur'}
  ],
  nickname: [
    {required: true, message: '请输入昵称', trigger: 'blur'},
    {min: 2, max: 10, message: '长度在 2 到 10 个字符之间', trigger: 'blur'}
  ],
  confirmPassword: [
    {required: true, message: '请再次输入密码', trigger: 'blur'},
    {validator:(rule, value, callback) => {
        if (value === '') {
          return callback(new Error('请输入密码'));
        }
        if (value !== registerForm.password) {
          return callback(new Error('两次密码输入不一致'));
        }
        callback();
      }, trigger:'blur'
    }
  ],
  //验证码校验逻辑
  captcha: [
    {required: true, message: '请输入验证码', trigger: 'blur'},
  ]
}

</script>
<template>
  <el-form
      label-position="top"
      label-width="auto"
      style="max-width: 600px"
      ref="formRef"
      :model="registerForm"
      status-icon
      :rules="rules"
      @keyup.enter="submitForm()"
  >
    <el-form-item label="用户名" prop="username">
      <el-input v-model="registerForm.username" type="text" placeholder="用户名"></el-input>
    </el-form-item>
    <el-form-item label="昵称" prop="nickname">
      <el-input v-model="registerForm.nickname" type="text" placeholder="昵称"></el-input>
    </el-form-item>
    <el-form-item label="密码" prop="password">
      <el-input v-model="registerForm.password" type="password" placeholder="密码"></el-input>
    </el-form-item>
    <el-form-item label="确认密码" prop="confirmPassword">
      <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码"></el-input>
    </el-form-item>
    <el-form-item label="验证码" prop="captcha">
      <div class="captcha-container">
        <el-input v-model="registerForm.captcha" type="text" placeholder="验证码" style="width: 50%"></el-input>
        <div class="captcha-img-container" @click="fetchCaptcha">
          <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-img"/>
          <div v-else class="captcha-loading">加载中...</div>
        </div>
      </div>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submitForm()">注册</el-button>
      <el-button @click="resetForm()">清空</el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.captcha-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.captcha-img-container {
  width: 45%;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.captcha-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.captcha-loading {
  color: #909399;
  font-size: 14px;
}
</style>
