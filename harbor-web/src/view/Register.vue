<script setup>
import { reactive, ref, onMounted } from "vue";
import { register, getCaptcha } from "../api/user.js";
import { useRouter } from "vue-router";

const router = useRouter();

const registerForm = reactive({
  username: "",
  password: "",
  nickname: "",
  confirmPassword: "",
  captcha: "",
  captchaKey: "",
});

const errors = reactive({
  username: "",
  password: "",
  nickname: "",
  confirmPassword: "",
  captcha: "",
});

const statusMessage = ref("");
const statusType = ref("info");
const formLoading = ref(false);
const captchaImage = ref("");

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
  errors.nickname = "";
  errors.confirmPassword = "";
  errors.captcha = "";

  if (!registerForm.username) {
    errors.username = "请输入用户名";
  } else if (
    registerForm.username.length < 3 ||
    registerForm.username.length > 20
  ) {
    errors.username = "用户名长度需在 3-20 之间";
  }

  if (!registerForm.nickname) {
    errors.nickname = "请输入昵称";
  } else if (
    registerForm.nickname.length < 3 ||
    registerForm.nickname.length > 20
  ) {
    errors.nickname = "昵称长度需在 3-20 之间";
  }

  if (!registerForm.password) {
    errors.password = "请输入密码";
  } else if (
    registerForm.password.length < 6 ||
    registerForm.password.length > 20
  ) {
    errors.password = "密码长度需在 6-20 之间";
  }

  if (!registerForm.confirmPassword) {
    errors.confirmPassword = "请再次输入密码";
  } else if (registerForm.confirmPassword !== registerForm.password) {
    errors.confirmPassword = "两次密码输入不一致";
  }

  if (!registerForm.captcha) {
    errors.captcha = "请输入验证码";
  }

  return (
    !errors.username &&
    !errors.password &&
    !errors.nickname &&
    !errors.confirmPassword &&
    !errors.captcha
  );
};

const fetchCaptcha = async () => {
  clearMessages();
  try {
    const data = await getCaptcha();
    if (data && data.captchaPic && data.captchaKey) {
      captchaImage.value = data.captchaPic;
      registerForm.captchaKey = data.captchaKey;
    } else {
      showMessage("获取验证码失败，请重试", "error");
    }
  } catch (error) {
    console.error("获取验证码出错:", error);
    showMessage("获取验证码失败，请重试", "error");
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
    await register(registerForm);
    showMessage("注册成功，正在跳转登录...", "success");
    router.push("/login");
  } catch (error) {
    console.error("注册失败:", error);
    showMessage("注册失败，请稍后重试", "error");
    await fetchCaptcha();
  } finally {
    formLoading.value = false;
  }
};

const resetForm = () => {
  registerForm.username = "";
  registerForm.password = "";
  registerForm.nickname = "";
  registerForm.confirmPassword = "";
  registerForm.captcha = "";
  errors.username = "";
  errors.password = "";
  errors.nickname = "";
  errors.confirmPassword = "";
  errors.captcha = "";
  clearMessages();
  fetchCaptcha();
};

const goToLogin = () => {
  router.push("/login");
};

onMounted(() => {
  fetchCaptcha();
});
</script>

<template>
  <div class="register-page">
    <div class="register-shell">
      <div class="panel">
        <div class="brand">
          <img class="logo" src="../assets/logo.svg" alt="Harbor" />
          <div class="title">创建 Harbor 账户</div>
        </div>

        <form class="form" @submit.prevent="submitForm">
          <div class="form-group" :class="{ invalid: errors.username }">
            <label for="username">用户名</label>
            <div class="input-wrap">
              <input
                id="username"
                type="text"
                v-model="registerForm.username"
                autocomplete="username"
                placeholder="请输入用户名"
                :disabled="formLoading"
              />
              <span class="input-hint" v-if="errors.username">{{
                errors.username
              }}</span>
            </div>
          </div>

          <div class="form-group" :class="{ invalid: errors.nickname }">
            <label for="nickname">昵称</label>
            <div class="input-wrap">
              <input
                id="nickname"
                type="text"
                v-model="registerForm.nickname"
                autocomplete="nickname"
                placeholder="请输入昵称"
                :disabled="formLoading"
              />
              <span class="input-hint" v-if="errors.nickname">{{
                errors.nickname
              }}</span>
            </div>
          </div>

          <div class="form-group" :class="{ invalid: errors.password }">
            <label for="password">密码</label>
            <div class="input-wrap">
              <input
                id="password"
                type="password"
                v-model="registerForm.password"
                autocomplete="new-password"
                placeholder="请输入密码"
                :disabled="formLoading"
              />
              <span class="input-hint" v-if="errors.password">{{
                errors.password
              }}</span>
            </div>
          </div>

          <div class="form-group" :class="{ invalid: errors.confirmPassword }">
            <label for="confirmPassword">确认密码</label>
            <div class="input-wrap">
              <input
                id="confirmPassword"
                type="password"
                v-model="registerForm.confirmPassword"
                autocomplete="new-password"
                placeholder="请再次输入密码"
                :disabled="formLoading"
              />
              <span class="input-hint" v-if="errors.confirmPassword">{{
                errors.confirmPassword
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
                  v-model="registerForm.captcha"
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
              <span v-if="!formLoading">注册</span>
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
            <span>已经有账号？</span>
            <button type="button" class="link" @click="goToLogin">
              去登录
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #33a2be;
  overflow: hidden;
  font-family: var(--theme-font-family, "Lato", sans-serif);
}

.register-shell {
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
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 13px;
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
  padding: 10px 12px;
  font-size: 14px;
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
  padding: 10px 12px;
  border-radius: 0;
  font-size: 13px;
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
  gap: 10px;
}

.btn {
  height: 42px;
  border-radius: 0;
  border: 1px solid transparent;
  font-weight: 600;
  letter-spacing: 0.1px;
  font-size: 14px;
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
  font-size: 13px;
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
  .register-shell {
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
  .register-shell {
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
