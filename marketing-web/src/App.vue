<template>
  <div id="app">
    <el-container direction="vertical">
      <!-- 顶部导航 -->
      <el-header height="60px" class="app-header">
        <div class="header-left">
          <h2 @click="router.push('/')" style="cursor: pointer;">
            <el-icon><ShoppingCart /></el-icon>
            XX 营销平台
          </h2>
        </div>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <span class="welcome">Hi, {{ userStore.nickname }}</span>
            <el-button text @click="$router.push('/my-orders')">我的优惠券</el-button>
            <el-button type="danger" text @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button type="primary" @click="$router.push('/login')">登录/注册</el-button>
          </template>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()

// 页面刷新后恢复登录态
onMounted(async () => {
  const token = localStorage.getItem('access_token')
  if (!token) return
  try {
    const res = await userApi.getMe()
    if (res.code === 200 && res.data) {
      userStore.setUser(res.data)
    }
  } catch (e) {
    // token 过期，清除
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
  }
})

const handleLogout = () => {
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eee;
  padding: 0 20px;
}

.header-left h2 {
  font-size: 20px;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome {
  color: #666;
  font-size: 14px;
}
</style>
