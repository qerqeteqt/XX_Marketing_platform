<template>
  <div class="moments-page">
    <!-- 发布帖子 -->
    <div class="post-editor">
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        placeholder="分享你的想法..."
        maxlength="2000"
        show-word-limit
        resize="none"
      />
      <div class="editor-footer">
        <span class="char-hint">{{ content.length }}/2000</span>
        <el-button type="primary" :loading="publishing" @click="handlePublish" :disabled="!content.trim()">
          发布
        </el-button>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div v-if="feed.length === 0" class="empty-state">
      <p>朋友圈暂无动态，去添加好友吧</p>
    </div>

    <div v-else class="feed-list">
      <div v-for="post in feed" :key="post.id" class="post-card">
        <div class="post-header">
          <span class="post-author">{{ post.nickname }}</span>
          <span class="post-time">{{ post.createTime }}</span>
        </div>
        <div class="post-content">{{ post.content }}</div>
        <div class="post-footer">
          <span class="like-btn" :class="{ liked: post.liked }" @click="handleLike(post)">
            <span v-if="post.liked">&#10084;</span>
            <span v-else>&#9825;</span>
            {{ post.likeCount || 0 }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { postApi } from '@/api'

const content = ref('')
const publishing = ref(false)
const feed = ref([])

onMounted(async () => {
  await loadFeed()
})

async function loadFeed() {
  try {
    const res = await postApi.getFeed()
    if (res.code === 200) feed.value = res.data || []
  } catch (_) {}
}

async function handlePublish() {
  if (!content.value.trim()) return
  publishing.value = true
  try {
    const res = await postApi.create(content.value)
    if (res.code === 200) {
      ElMessage.success('发布成功')
      content.value = ''
      await loadFeed()
    }
  } finally { publishing.value = false }
}

async function handleLike(post) {
  try {
    const res = await postApi.toggleLike(post.id)
    if (res.code === 200) {
      post.liked = !post.liked
      post.likeCount += post.liked ? 1 : -1
    }
  } catch (_) {}
}
</script>

<style scoped>
.moments-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 20px 0;
}
.post-editor {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}
.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}
.char-hint {
  font-size: 12px;
  color: #c0c4cc;
}
.empty-state {
  text-align: center;
  color: #909399;
  padding: 80px 0;
  font-size: 15px;
}
.feed-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.post-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}
.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.post-author {
  font-weight: 500;
  font-size: 15px;
  color: #303133;
}
.post-time {
  font-size: 12px;
  color: #c0c4cc;
}
.post-content {
  font-size: 15px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 16px;
}
.post-footer {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
.like-btn {
  cursor: pointer;
  font-size: 14px;
  color: #909399;
  user-select: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color .2s;
}
.like-btn.liked {
  color: #e24b4a;
}
.like-btn:hover {
  color: #e24b4a;
}
</style>
