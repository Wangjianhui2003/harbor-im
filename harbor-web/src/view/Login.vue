<script setup>
import { TERMINAL_TYPE } from "../common/enums.js";
import { onMounted, reactive, ref } from "vue";
import { getCaptcha, login } from "../api/user.js";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";

const router = useRouter();

//登录表单
const loginFormRef = ref();

//表单数据
const loginForm = reactive({
    terminal: TERMINAL_TYPE.WEB,
    username: "",
    password: "",
    captcha: "",
    captchaKey: "",
});

const formLoading = ref(false);

//验证码图片
const captchaImage = ref("");

const rules = {
    //校验规则
    username: [
        { required: true, message: "请输入用户名", trigger: "blur" },
        { min: 3, max: 20, message: "长度在 3 到 20 个字符之间", trigger: "blur" },
    ],
    password: [
        { required: true, message: "请输入密码", trigger: "blur" },
        { min: 6, max: 20, message: "长度在 6 到 20 个字符之间", trigger: "blur" },
    ],
    captcha: [{ required: true, message: "请输入验证码", trigger: "blur" }],
};

//获取验证码
const fetchCaptcha = async () => {
    try {
        const data = await getCaptcha();
        if (data && data.captchaPic && data.captchaKey) {
            captchaImage.value = data.captchaPic;
            loginForm.captchaKey = data.captchaKey;
        } else {
            ElMessage.error("获取验证码失败");
        }
    } catch (error) {
        console.error("获取验证码出错:", error);
        ElMessage.error("获取验证码失败");
    }
};

//提交登录表单
const submitForm = () => {
    if (!loginFormRef.value) return;
    loginFormRef.value.validate((valid) => {
        if (valid) {
            formLoading.value = true;
            login(loginForm)
                .then((data) => {
                    // 保存密码到cookie(不安全)
                    setCookie("username", loginForm.username);
                    setCookie("password", loginForm.password);
                    // 保存token
                    sessionStorage.setItem("accessToken", data.accessToken);
                    sessionStorage.setItem("refreshToken", data.refreshToken);
                    formLoading.value = false;
                    ElMessage.success("登录成功");
                    router.push("/home/chat");
                })
                .finally(() => {
                    formLoading.value = false;
                });
        } else {
            ElMessage.error("请检查输入");
        }
    });
    formLoading.value = false;
};

const getCookie = (name) => {
    let reg = new RegExp("(^| )" + name + "=([^;]*)(;|$)");
    let arr = document.cookie.match(reg);
    if (arr) {
        return decodeURIComponent(arr[2]);
    }
    return "";
};

const setCookie = (name, value) => {
    document.cookie = name + "=" + decodeURIComponent(value);
};

const resetForm = () => {
    loginFormRef.value.resetFields();
    fetchCaptcha(); // 重置表单时刷新验证码
};

const goToRegister = () => {
    router.push("/register");
};

onMounted(() => {
    loginForm.username = getCookie("username") || "";
    // cookie存密码不安全，只是临时方便
    loginForm.password = getCookie("password") || "";
    fetchCaptcha();
});
</script>

<template>
    <div class="login-window">
        <div class="login-panel">
            <div class="login-header">
                <img class="logo" src="../assets/logo.png" alt="" />
                <div class="title">欢迎使用Harbor</div>
            </div>
            <el-form class="login-form" :model="loginForm" :rules="rules" ref="loginFormRef" @keyup.enter="submitForm()"
                label-position="top" label-width="auto" v-loading.fullscreen.lock="formLoading">
                <el-form-item label="用户名" prop="username">
                    <el-input class="username-input" clearable size="large" type="text" v-model="loginForm.username"
                        autocomplete="off" placeholder="请输入用户名">
                    </el-input>
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input class="password-input" clearable size="large" show-password type="password"
                        v-model="loginForm.password" autocomplete="off" placeholder="请输入密码">
                    </el-input>
                </el-form-item>
                <el-form-item label="验证码" prop="captcha">
                    <div class="captcha-container">
                        <el-input class="captcha-input" clearable size="large" v-model="loginForm.captcha" type="text"
                            placeholder="验证码">
                        </el-input>
                        <div class="captcha-img-container" @click="fetchCaptcha">
                            <img class="captcha-img" v-if="captchaImage" :src="captchaImage" alt="验证码" />
                            <div v-else class="captcha-img-loading">加载中...</div>
                        </div>
                    </div>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" class="login-button" @click="submitForm">登录</el-button>
                    <el-button class="reset-button" @click="resetForm">重置</el-button>
                </el-form-item>
                <el-form-item>
                    <div class="register-tip">
                        还没有账号？
                        <el-button type="text" class="register-link" @click="goToRegister">点击注册</el-button>
                    </div>
                </el-form-item>
            </el-form>
        </div>
    </div>
</template>

<style scoped lang="scss">
.login-window {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    background-color: #dcdfe6;
}

.login-panel {
    padding: 20px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    box-shadow: rgba(17, 12, 46, 0.15) 0px 48px 100px 0px;
    border-radius: 30px;
    background-color: white;

    .login-header {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;

        .logo {
            width: 60px;
        }

        .title {}
    }

    .login-form {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        width: 400px;

        .username-input,
        .password-input {
            width: 300px;
        }
    }

    .captcha-container {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 300px;

        .captcha-input {
            width: 55%;
        }

        .captcha-img-container {
            width: 35%;
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

        .captcha-img-loading {
            color: #909399;
            font-size: 14px;
        }
    }
}
</style>
