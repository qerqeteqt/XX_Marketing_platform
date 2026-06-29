<template>
  <div class="home-page">
    <div class="banner">
      <h1>🔥 XX 全域营销平台</h1>
      <p>精选好店，进店领券，畅享优惠</p>
    </div>

    <section class="section">
      <div class="section-header">
        <h3>热门商家</h3>
        <el-button text type="primary" @click="goCoupons">查看全部 →</el-button>
      </div>
      <el-row :gutter="20">
        <el-col :span="6" v-for="m in merchants" :key="m.id">
          <div class="merchant-item" @click="goMerchant(m.id)">
            <div class="merchant-icon">{{ m.logo || m.shopName?.charAt(0) }}</div>
            <h4>{{ m.shopName }}</h4>
            <p class="desc">{{ m.description }}</p>
            <div class="meta">
              <span>⭐ {{ m.rating }}</span>
              <span>月售 {{ m.sales }}+</span>
            </div>
            <el-button size="small" type="primary" circle class="go-btn">→</el-button>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { merchantApi } from '@/api'

const router = useRouter()
const merchants = ref([])

function goMerchant(id) { router.push(`/merchant/${id}`) }
function goCoupons() { router.push('/coupons') }

onMounted(async () => {
  try {
    const res = await merchantApi.hot()
    if (res.code === 200) merchants.value = res.data || []
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.home-page { max-width: 1200px; margin: 0 auto; }

.banner {
  text-align: center;
  padding: 80px 0;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 50%, #ff6348 100%);
  border-radius: 20px;
  margin-bottom: 50px;
  color: #fff;
}
.banner h1 { font-size: 38px; margin-bottom: 12px; font-weight: 800; }
.banner p { font-size: 18px; opacity: 0.92; }

.section { margin-bottom: 50px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.section-header h3 { font-size: 24px; color: #303133; font-weight: 700; }

.merchant-item {
  background: #fff;
  border-radius: 16px;
  padding: 28px 20px;
  text-align: center;
  cursor: pointer;
  position: relative;
  transition: all .25s;
  box-shadow: 0 2px 16px rgba(0,0,0,.06);
}
.merchant-item:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 30px rgba(0,0,0,.12);
}

.merchant-icon {
  width: 72px; height: 72px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f5f7fa, #e4e7ed);
  font-size: 36px;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px;
}

.merchant-item h4 { font-size: 17px; margin-bottom: 6px; color: #303133; }
.desc { font-size: 13px; color: #909399; margin-bottom: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta { display: flex; justify-content: center; gap: 16px; font-size: 13px; color: #606266; }

.go-btn {
  position: absolute;
  right: 12px;
  bottom: 12px;
  opacity: 0;
  transition: opacity .2s;
}
.merchant-item:hover .go-btn { opacity: 1; }
</style>
