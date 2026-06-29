<template>
  <div class="login-container">
    <div class="login-card">
      <h2>{{ isRegister ? '注册新账号' : '登录' }}</h2>
      <p class="subtitle">{{ isRegister ? '创建账号，立即领券' : '领取超值优惠券，畅享商家服务' }}</p>

      <el-form ref="formRef" :model="form" :rules="currentRules" label-width="0" size="large">
        <!-- 手机号（所有模式共用） -->
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>

        <!-- 注册模式 -->
        <template v-if="isRegister">
          <el-form-item prop="nickname">
            <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="设置密码（6-20位）" show-password />
          </el-form-item>
        </template>

        <!-- 验证码登录 -->
        <template v-else-if="loginMode === 'sms'">
          <el-form-item prop="code">
            <div style="display:flex;gap:10px">
              <el-input v-model="form.code" placeholder="验证码" maxlength="6" style="flex:1" />
              <el-button :disabled="countdown > 0" :loading="sending" @click="sendCode" style="width:130px">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
        </template>

        <!-- 密码登录 -->
        <template v-else>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit" style="width:100%">
            {{ isRegister ? '注 册' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="switch-mode">
        <template v-if="!isRegister">
          <el-button link type="primary" @click="swapMode">
            {{ loginMode === 'sms' ? '密码登录' : '验证码登录' }}
          </el-button>
          <span class="divider">|</span>
        </template>
        <el-button link type="primary" @click="toggleRegister">
          {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const isRegister = ref(false)
const loginMode = ref('sms')
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const formRef = ref(null)

const form = reactive({
  phone: '',
  code: '',
  password: '',
  nickname: ''
})

const baseRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const currentRules = computed(() => {
  if (isRegister.value) {
    return {
      ...baseRules,
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
      ],
      nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
    }
  }
  if (loginMode.value === 'sms') {
    return {
      ...baseRules,
      code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
    }
  }
  return {
    ...baseRules,
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }
})

function swapMode() {
  loginMode.value = loginMode.value === 'sms' ? 'password' : 'sms'
  form.code = ''
  form.password = ''
}

function toggleRegister() {
  isRegister.value = !isRegister.value
  form.code = ''
  form.password = ''
  form.nickname = ''
}

async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  sending.value = true
  try {
    await userApi.sendCode(form.phone)
    ElMessage.success('验证码已发送')
    countdown.value = 60
    const timer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(timer) }, 1000)
  } finally { sending.value = false }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }

  loading.value = true
  try {
    if (isRegister.value) {
      const res = await userApi.register({ phone: form.phone, password: form.password, nickname: form.nickname })
      if (res.code === 200) {
        ElMessage.success('注册成功，请登录')
        isRegister.value = false
        loginMode.value = 'password'
        form.password = ''
        form.nickname = ''
      }
    } else {
      const payload = { phone: form.phone, loginType: loginMode.value === 'sms' ? 'SMS' : 'PASSWORD' }
      if (loginMode.value === 'sms') payload.code = form.code
      else payload.password = form.password

      const res = await userApi.login(payload)
      if (res.code === 200) {
        localStorage.setItem('access_token', res.data.accessToken)
        localStorage.setItem('refresh_token', res.data.refreshToken)
        try {
          const me = await userApi.getMe()
          if (me.code === 200) userStore.setUser(me.data)
        } catch (_) {}
        ElMessage.success('登录成功')
        router.push('/home')
      }
    }
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-container {
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: -20px;
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0,0,0,.15);
}
.login-card h2 { text-align: center; color: #303133; margin-bottom: 8px; }
.subtitle { text-align: center; color: #909399; font-size: 14px; margin-bottom: 30px; }
.switch-mode { text-align: center; margin-top: 8px; }
.divider { margin: 0 8px; color: #dcdfe6; }
</style>
