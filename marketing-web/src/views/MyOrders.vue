<template>
  <div class="orders-page">
    <h2 class="page-title">📋 我的优惠券</h2>

    <div v-if="!loading && orders.length > 0">
      <div class="order-item" v-for="order in orders" :key="order.id">
        <div class="order-left">
          <div class="order-amount">
            <span class="currency">¥</span>
            <span class="value">{{ order.amount }}</span>
          </div>
        </div>
        <div class="order-info">
          <div class="order-no">订单号: {{ order.orderNo }}</div>
          <div class="order-time">领取时间: {{ order.createTime }}</div>
          <div class="order-expire" v-if="order.expireTime">
            有效期至: {{ order.expireTime }}
          </div>
        </div>
        <div class="order-right">
          <el-tag :type="statusTag(order.status)" size="large">
            {{ statusText(order.status) }}
          </el-tag>
          <el-button
            v-if="order.status === 0"
            type="primary"
            size="small"
            @click="handleUse(order)"
            style="margin-top: 12px"
          >
            立即使用
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-else-if="!loading" description="暂无优惠券">
      <el-button type="primary" @click="$router.push('/coupons')">去抢券</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { orderApi } from '@/api'
import { ElMessage } from 'element-plus'

const orders = ref([])
const loading = ref(false)

const statusText = (s) => ({ 0: '未使用', 1: '已使用', 2: '已过期' })[s] || '未知'
const statusTag = (s) => ({ 0: 'success', 1: 'info', 2: 'warning' })[s] || 'info'

onMounted(async () => {
  loading.value = true
  try {
    const res = await orderApi.myOrders()
    if (res.code === 200) orders.value = res.data || []
  } finally {
    loading.value = false
  }
})

const handleUse = async (order) => {
  try {
    const res = await orderApi.useCoupon(order.id)
    if (res.code === 200) {
      ElMessage.success('使用成功')
      order.status = 1
    }
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.orders-page { max-width: 900px; margin: 0 auto; }
.page-title { font-size: 28px; margin-bottom: 30px; color: #303133; }

.order-item {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
}

.order-left {
  width: 120px;
  text-align: center;
  border-right: 2px dashed #eee;
  padding-right: 24px;
  margin-right: 24px;
}

.order-amount .currency { font-size: 16px; color: #ff6348; }
.order-amount .value { font-size: 32px; font-weight: bold; color: #ff6348; }

.order-info { flex: 1; font-size: 14px; color: #606266; line-height: 2; }

.order-right { text-align: center; }
</style>
