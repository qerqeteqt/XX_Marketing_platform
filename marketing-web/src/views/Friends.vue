<template>
  <div class="friends-page">
    <el-tabs v-model="activeTab" class="friends-tabs">
      <!-- 好友列表 -->
      <el-tab-pane label="好友列表" name="list">
        <div v-if="friends.length === 0" class="empty-state">
          <p>还没有好友，去添加吧</p>
        </div>
        <div v-else class="friend-list">
          <div v-for="f in friends" :key="f.fromUserId + '_' + f.fromPhone" class="friend-item">
            <span class="friend-name">{{ f.fromNickname || f.fromPhone }}</span>
            <span class="friend-phone">{{ f.fromPhone }}</span>
          </div>
        </div>
      </el-tab-pane>

      <!-- 添加好友 -->
      <el-tab-pane label="添加好友" name="add">
        <div class="add-friend-section">
          <el-input v-model="addPhone" placeholder="输入对方的手机号" maxlength="11" size="large" style="flex:1" />
          <el-button type="primary" size="large" :loading="adding" @click="handleAddFriend">添加好友</el-button>
        </div>
      </el-tab-pane>

      <!-- 好友申请 -->
      <el-tab-pane :label="'好友申请' + (received.length ? ' (' + received.length + ')' : '')" name="requests">
        <div v-if="received.length === 0" class="empty-state">
          <p>暂无好友申请</p>
        </div>
        <div v-else class="request-list">
          <div v-for="req in received" :key="req.id" class="request-item">
            <div class="request-info">
              <span class="request-name">{{ req.fromNickname || req.fromPhone }}</span>
              <span class="request-phone">{{ req.fromPhone }}</span>
            </div>
            <div class="request-actions">
              <el-button type="success" size="small" @click="handleRequest(req.id, true)">接受</el-button>
              <el-button type="danger" size="small" @click="handleRequest(req.id, false)">拒绝</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { friendApi } from '@/api'

const activeTab = ref('list')
const addPhone = ref('')
const adding = ref(false)
const friends = ref([])
const received = ref([])

onMounted(async () => {
  await loadFriends()
  await loadRequests()
})

async function loadFriends() {
  try {
    const res = await friendApi.getFriendList()
    if (res.code === 200) friends.value = res.data || []
  } catch (_) {}
}

async function loadRequests() {
  try {
    const res = await friendApi.getReceivedRequests()
    if (res.code === 200) received.value = res.data || []
  } catch (_) {}
}

async function handleAddFriend() {
  if (!/^1[3-9]\d{9}$/.test(addPhone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  adding.value = true
  try {
    const res = await friendApi.sendRequest(addPhone.value)
    if (res.code === 200) {
      ElMessage.success(res.message || '好友申请已发送')
      addPhone.value = ''
    }
  } finally { adding.value = false }
}

async function handleRequest(requestId, accept) {
  try {
    const res = await friendApi.handleRequest(requestId, accept)
    if (res.code === 200) {
      ElMessage.success(res.message)
      await loadFriends()
      await loadRequests()
    }
  } catch (_) {}
}
</script>

<style scoped>
.friends-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px 0;
}
.friends-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}
.empty-state {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  font-size: 15px;
}
.friend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.friend-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}
.friend-name {
  font-weight: 500;
  font-size: 15px;
  color: #303133;
}
.friend-phone {
  font-size: 13px;
  color: #909399;
}
.add-friend-section {
  display: flex;
  gap: 12px;
  align-items: center;
}
.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.request-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}
.request-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.request-name {
  font-weight: 500;
  font-size: 15px;
  color: #303133;
}
.request-phone {
  font-size: 13px;
  color: #909399;
}
.request-actions {
  display: flex;
  gap: 8px;
}
</style>
