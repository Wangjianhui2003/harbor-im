<script setup>
import { TERMINAL_TYPE } from "../common/enums.js";
import { onMounted, reactive, ref } from "vue";
import { getCaptcha, login } from "../api/user.js";
import { useRouter } from "vue-router";

const router = useRouter();

const loginForm = reactive({
  terminal: TERMINAL_TYPE.WEB,
  username: "",
  password: "",
  captcha: "",
  captchaKey: "",
});

const errors = reactive({
  username: "",
  password: "",
  captcha: "",
});

const statusMessage = ref("");
const statusType = ref("info");
const formLoading = ref(false);
const captchaImage = ref("");

const setCookie = (name, value) => {
  document.cookie = name + "=" + decodeURIComponent(value);
};

const getCookie = (name) => {
  const reg = new RegExp("(^| )" + name + "=([^;]*)(;|$)");
  const arr = document.cookie.match(reg);
  if (arr) {
    return decodeURIComponent(arr[2]);
  }
  return "";
};

const clearMessages = () => {
  statusMessage.value = "";
  statusType.value = "info";
};

const showMessage = (message, type = "info") => {
  statusMessage.value = message;
  statusType.value = type;
};

const validate = () => {
  errors.username = "";
  errors.password = "";
  errors.captcha = "";

  if (!loginForm.username) {
    errors.username = "请输入用户名";
  } else if (loginForm.username.length < 3 || loginForm.username.length > 20) {
    errors.username = "用户名长度需在 3-20 之间";
  }

  if (!loginForm.password) {
    errors.password = "请输入密码";
  } else if (loginForm.password.length < 6 || loginForm.password.length > 20) {
    errors.password = "密码长度需在 6-20 之间";
  }

  if (!loginForm.captcha) {
    errors.captcha = "请输入验证码";
  }

  return !errors.username && !errors.password && !errors.captcha;
};

const fetchCaptcha = async () => {
  clearMessages();
  try {
    const data = await getCaptcha();
    if (data && data.captchaPic && data.captchaKey) {
      captchaImage.value = data.captchaPic;
      loginForm.captchaKey = data.captchaKey;
    } else {
      showMessage("验证码获取失败，请重试", "error");
    }
  } catch (error) {
    console.error("获取验证码出错:", error);
    showMessage("验证码获取失败，请重试", "error");
  }
};

const submitForm = async () => {
  clearMessages();
  if (!validate()) {
    showMessage("请完善表单后重试", "error");
    return;
  }
  formLoading.value = true;
  try {
    const data = await login(loginForm);
    setCookie("username", loginForm.username);
    setCookie("password", loginForm.password);
    sessionStorage.setItem("accessToken", data.accessToken);
    sessionStorage.setItem("refreshToken", data.refreshToken);
    showMessage("登录成功，正在跳转...", "success");
    router.push("/home");
  } catch (error) {
    console.error("登录失败:", error);
    showMessage("登录失败，请检查输入或稍后再试", "error");
    await fetchCaptcha();
  } finally {
    formLoading.value = false;
  }
};

const resetForm = () => {
  loginForm.username = "";
  loginForm.password = "";
  loginForm.captcha = "";
  errors.username = "";
  errors.password = "";
  errors.captcha = "";
  clearMessages();
  fetchCaptcha();
};

const goToRegister = () => {
  router.push("/register");
};

onMounted(() => {
  loginForm.username = getCookie("username") || "";
  loginForm.password = getCookie("password") || "";
  fetchCaptcha();
});
</script>

<template>
  <div class="login-page">
    <div class="login-shell">
      <div class="panel">
        <div class="brand">
          <img class="logo" src="../assets/logo.svg" alt="Harbor" />
          <div class="title">欢迎使用 Harbor</div>
        </div>

        <form class="form" @submit.prevent="submitForm">
          <div class="form-group" :class="{ invalid: errors.username }">
            <label for="username">用户名</label>
            <div class="input-wrap">
              <input
                id="username"
                type="text"
                v-model="loginForm.username"
                autocomplete="username"
                placeholder="请输入用户名"
                :disabled="formLoading"
              />
              <span class="input-hint" v-if="errors.username">{{
                errors.username
              }}</span>
            </div>
          </div>

          <div class="form-group" :class="{ invalid: errors.password }">
            <label for="password">密码</label>
            <div class="input-wrap">
              <input
                id="password"
                type="password"
                v-model="loginForm.password"
                autocomplete="current-password"
                placeholder="请输入密码"
                :disabled="formLoading"
              />
              <span class="input-hint" v-if="errors.password">{{
                errors.password
              }}</span>
            </div>
          </div>

          <div class="form-group" :class="{ invalid: errors.captcha }">
            <label for="captcha">验证码</label>
            <div class="captcha-row">
              <div class="input-wrap">
                <input
                  id="captcha"
                  type="text"
                  v-model="loginForm.captcha"
                  placeholder="请输入验证码"
                  :disabled="formLoading"
                />
                <span class="input-hint" v-if="errors.captcha">{{
                  errors.captcha
                }}</span>
              </div>
              <button
                class="captcha-refresh"
                type="button"
                @click="fetchCaptcha"
                :disabled="formLoading"
              >
                <img
                  v-if="captchaImage"
                  class="captcha-img"
                  :src="captchaImage"
                  alt="验证码"
                />
                <span v-else class="captcha-loading">加载中...</span>
              </button>
            </div>
          </div>

          <div class="status" v-if="statusMessage" :data-type="statusType">
            <span>{{ statusMessage }}</span>
          </div>

          <div class="action-row">
            <button type="submit" class="btn primary" :disabled="formLoading">
              <span v-if="!formLoading">登录</span>
              <span v-else class="loader"></span>
            </button>
            <button
              type="button"
              class="btn ghost"
              @click="resetForm"
              :disabled="formLoading"
            >
              重置
            </button>
          </div>

          <div class="foot">
            <span>还没有账号？</span>
            <button type="button" class="link" @click="goToRegister">
              点击注册
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #33a2be;
  overflow: hidden;
  font-family: var(--theme-font-family, "Lato", sans-serif);
}

.login-shell {
  position: relative;
  width: min(900px, 92vw);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 24px;
  padding: 34px;
  border-radius: 0;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
  color: #374151;
}

.panel {
  grid-column: span 2;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  align-items: center;
}

.brand {
  color: #111827;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.brand .logo {
  width: 64px;
  height: 64px;
}

.brand .title {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand .subtitle {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
  max-width: 320px;
}

.form {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 0;
  padding: 26px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  color: #374151;
  letter-spacing: 0.2px;
}

.input-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form input {
  width: 100%;
  padding: 12px 14px;
  border-radius: 0;
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #111827;
  outline: none;
  transition:
    border 0.2s ease,
    background 0.2s ease,
    box-shadow 0.2s ease;
}

.form input:focus {
  border-color: #2563eb;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.15);
}

.form input::placeholder {
  color: #9ca3af;
}

.input-hint {
  color: #fca5a5;
  font-size: 12px;
}

.form-group.invalid input {
  border-color: #ef4444;
  background: #ffffff;
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 130px;
  gap: 10px;
  align-items: stretch;
}

.captcha-refresh {
  border: 1px dashed #cbd5e1;
  background: #ffffff;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    border 0.2s ease,
    transform 0.2s ease;
  padding: 0;
}

.captcha-refresh:hover {
  border-color: #2563eb;
  transform: translateY(-1px);
}

.captcha-refresh:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 0;
}

.captcha-loading {
  color: #6b7280;
  font-size: 13px;
}

.status {
  padding: 12px 14px;
  border-radius: 0;
  font-size: 14px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
}

.status[data-type="success"] {
  border-color: #10b981;
  color: #065f46;
  background: #d1fae5;
}

.status[data-type="error"] {
  border-color: #ef4444;
  color: #7f1d1d;
  background: #fee2e2;
}

.action-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.btn {
  height: 46px;
  border-radius: 0;
  border: 1px solid transparent;
  font-weight: 600;
  letter-spacing: 0.2px;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    opacity 0.15s ease;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.btn.primary {
  background: #3871ee;
  color: #ffffff;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.25);
  border-color: transparent;
}

.btn.primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.35);
}

.btn.ghost {
  background: transparent;
  border-color: #d1d5db;
  color: #374151;
}

.btn.ghost:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: #94a3b8;
}

.loader {
  width: 18px;
  height: 18px;
  border: 3px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  display: inline-block;
  animation: spin 0.9s linear infinite;
}

.foot {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #676d74;
  font-size: 14px;
}

.link {
  background: none;
  border: none;
  color: #386bd9;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.link:hover {
  text-decoration: underline;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .panel {
    grid-template-columns: 1fr;
    grid-column: span 1;
  }

  .brand {
    text-align: center;
    align-items: center;
  }
}

@media (max-width: 640px) {
  .login-shell {
    padding: 22px;
  }

  .form {
    padding: 20px;
  }

  .action-row {
    grid-template-columns: 1fr;
  }

  .captcha-row {
    grid-template-columns: 1fr;
  }
}
</style>
