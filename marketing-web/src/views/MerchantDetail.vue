<template>
  <div class="merchant-detail">
    <div class="back-link">
      <el-button text @click="goBack">← 返回</el-button>
    </div>

    <div v-if="loading" style="text-align:center;padding:60px">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p style="margin-top:12px;color:#909399">加载中...</p>
    </div>

    <div v-else-if="error" style="text-align:center;padding:60px">
      <p style="color:#f56c6c;font-size:16px">{{ error }}</p>
      <el-button @click="loadData" style="margin-top:16px">重试</el-button>
    </div>

    <div v-else-if="merchant" class="merchant-content">
      <div class="merchant-header">
        <div class="merchant-avatar">{{ merchant.logo || merchant.shopName?.charAt(0) }}</div>
        <div class="merchant-info">
          <h1>{{ merchant.shopName }}</h1>
          <p>{{ merchant.description }}</p>
          <div class="meta">
            <el-tag>{{ merchant.type }}</el-tag>
            <span>⭐ {{ merchant.rating }}</span>
            <span>已售 {{ merchant.sales }}+</span>
          </div>
          <div class="detail-row" v-if="merchant.address">
            <span>📍 {{ merchant.address }}</span>
          </div>
          <div class="detail-row" v-if="merchant.businessHours">
            <span>🕐 {{ merchant.businessHours }}</span>
          </div>
        </div>
        <div class="follow-action">
          <el-button
            :type="isFollowed ? 'default' : 'primary'"
            @click="toggleFollow"
            :loading="followLoading"
          >
            {{ isFollowed ? '已关注' : '+ 关注' }}
          </el-button>
        </div>
      </div>

      <div class="merchant-coupons">
        <h3>🎫 本店优惠券</h3>
        <el-row :gutter="16">
          <el-col :span="8" v-for="c in coupons" :key="c.id">
            <div class="coupon-card">
              <div class="coupon-amount">¥{{ c.amount }}</div>
              <div class="coupon-body">
                <h4>{{ c.name }}</h4>
                <p v-if="c.minAmount > 0">满 ¥{{ c.minAmount }} 可用</p>
              </div>
              <div class="coupon-stock">剩余 {{ c.remainStock }} 张</div>
              <el-button
                type="danger"
                size="small"
                :disabled="c.remainStock <= 0"
                :loading="grabbingId === c.id"
                @click="grabCoupon(c)"
                style="width:100%"
              >
                {{ c.remainStock > 0 ? '立即抢' : '已抢完' }}
              </el-button>
            </div>
          </el-col>
        </el-row>
        <el-empty v-if="coupons.length === 0" description="暂无优惠券" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { merchantApi, couponApi, userApi } from '@/api'

const route = useRoute()
const router = useRouter()

const merchant = ref(null)
const coupons = ref([])
const isFollowed = ref(false)
const followLoading = ref(false)
const grabbingId = ref(null)
const loading = ref(true)
const error = ref('')
let pollTimer = null

function goBack() { router.back() }

async function loadData() {
  loading.value = true
  error.value = ''
  const merchantId = route.params.id

  // 先加载商家信息（已有旧接口）
  try {
    const res = await merchantApi.detail(merchantId)
    if (res.code === 200) merchant.value = res.data
  } catch (e) {
    error.value = '加载商家信息失败，请确认后端已启动'
    loading.value = false
    return
  }

  // 再加载优惠券（新接口，失败也不影响商家展示）
  try {
    const res = await couponApi.listByMerchant(merchantId)
    if (res.code === 200) coupons.value = res.data || []
  } catch (e) {
    console.warn('加载优惠券失败（可能是后端未重启）:', e)
  }

  // 检查关注状态
  try {
    const token = localStorage.getItem('access_token')
    if (token && merchant.value) {
      const res = await userApi.isFollowing(merchant.value.userId)
      if (res.code === 200) isFollowed.value = res.data
    }
  } catch (e) { /* ignore */ }

  loading.value = false

  // 启动定时轮询（每3秒刷新优惠券库存）
  pollTimer = setInterval(loadCoupons, 3000)
}

onMounted(loadData)

// 离开页面时清除定时器
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

const toggleFollow = async () => {
  const token = localStorage.getItem('access_token')
  if (!token) { ElMessage.warning('请先登录'); return }
  followLoading.value = true
  try {
    if (isFollowed.value) {
      await userApi.unfollow(merchant.value.userId)
      ElMessage.success('已取消关注')
    } else {
      await userApi.follow(merchant.value.userId)
      ElMessage.success('关注成功')
    }
    isFollowed.value = !isFollowed.value
  } finally { followLoading.value = false }
}

const grabCoupon = async (coupon) => {
  const token = localStorage.getItem('access_token')
  if (!token) { ElMessage.warning('请先登录'); return }
  grabbingId.value = coupon.id
  try {
    const res = await couponApi.grab(coupon.id)
    if (res.code === 200) {
      ElMessage.success('抢券成功！')
      // 重新加载优惠券列表，确保显示真实库存
      await loadCoupons()
    }
  } finally { grabbingId.value = null }
}

async function loadCoupons() {
  try {
    const res = await couponApi.listByMerchant(route.params.id)
    if (res.code === 200) coupons.value = res.data || []
  } catch (e) { console.error(e) }
}
</script>

<style scoped>
.merchant-detail { max-width: 960px; margin: 0 auto; }
.back-link { margin-bottom: 16px; }

.merchant-header {
  display: flex;
  gap: 24px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
  margin-bottom: 30px;
  align-items: flex-start;
}

.merchant-avatar {
  width: 80px; height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f5f7fa, #e4e7ed);
  font-size: 40px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.merchant-info { flex: 1; }
.merchant-info h1 { font-size: 24px; margin-bottom: 8px; }
.merchant-info > p { color: #606266; margin-bottom: 12px; }
.meta { display: flex; gap: 16px; align-items: center; margin-bottom: 12px; font-size: 14px; color: #606266; }
.detail-row { font-size: 14px; color: #909399; margin-top: 6px; }

.follow-action { flex-shrink: 0; padding-top: 8px; }

.merchant-coupons h3 { font-size: 20px; margin-bottom: 16px; color: #303133; }

.coupon-card {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  border-radius: 12px;
  padding: 20px;
  color: #fff;
  text-align: center;
  margin-bottom: 16px;
}
.coupon-amount { font-size: 36px; font-weight: bold; margin-bottom: 8px; }
.coupon-amount::before { content: '¥'; font-size: 18px; margin-right: 2px; }
.coupon-body h4 { font-size: 14px; margin-bottom: 4px; }
.coupon-body p { font-size: 12px; opacity: 0.85; }
.coupon-stock { font-size: 12px; margin: 10px 0 8px; opacity: 0.85; }
</style>
