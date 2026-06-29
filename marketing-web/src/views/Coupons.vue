<template>
  <div class="coupons-page">
    <h2 class="page-title">🏪 全部商家</h2>
    <p class="page-subtitle">点击商家卡片进入店铺，查看并领取专属优惠券</p>

    <el-row :gutter="24">
      <el-col :span="8" v-for="m in merchants" :key="m.id">
        <div class="merchant-card" @click="goMerchant(m.id)">
          <div class="card-top">
            <div class="avatar">{{ m.logo || m.shopName?.charAt(0) }}</div>
            <div class="info">
              <h4>{{ m.shopName }}</h4>
              <p>{{ m.description }}</p>
            </div>
          </div>
          <div class="card-bottom">
            <el-tag size="small" effect="plain">{{ m.type }}</el-tag>
            <span class="stat">⭐ {{ m.rating }}</span>
            <span class="stat">月售 {{ m.sales }}</span>
          </div>
          <div class="overlay">点击进店 →</div>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && merchants.length === 0" description="暂无商家" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { merchantApi } from '@/api'

const router = useRouter()
const merchants = ref([])
const loading = ref(false)

function goMerchant(id) { router.push(`/merchant/${id}`) }

onMounted(async () => {
  loading.value = true
  try {
    const res = await merchantApi.hot()
    if (res.code === 200) merchants.value = res.data || []
  } finally { loading.value = false }
})
</script>

<style scoped>
.coupons-page { max-width: 1080px; margin: 0 auto; }
.page-title { font-size: 28px; margin-bottom: 6px; color: #303133; font-weight: 700; }
.page-subtitle { color: #909399; margin-bottom: 32px; font-size: 15px; }

.merchant-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  margin-bottom: 24px;
  cursor: pointer;
  position: relative;
  box-shadow: 0 2px 14px rgba(0,0,0,.05);
  transition: all .25s;
}
.merchant-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 28px rgba(0,0,0,.1);
}
.merchant-card:hover .overlay {
  opacity: 1;
}

.card-top {
  display: flex;
  padding: 24px 20px 16px;
  gap: 16px;
  align-items: center;
}

.avatar {
  width: 60px; height: 60px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f5f7fa, #e4e7ed);
  font-size: 30px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.info h4 { font-size: 17px; margin-bottom: 4px; color: #303133; }
.info p { font-size: 13px; color: #909399; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 220px; }

.card-bottom {
  padding: 12px 20px 18px;
  display: flex;
  gap: 14px;
  align-items: center;
  font-size: 13px;
  color: #606266;
  border-top: 1px solid #f5f5f5;
}

.stat { display: flex; align-items: center; gap: 4px; }

.overlay {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(102, 126, 234, 0.88);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700;
  opacity: 0; transition: opacity .25s;
  pointer-events: none;
}
</style>
