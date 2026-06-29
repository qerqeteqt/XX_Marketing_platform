import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/coupons',
    name: 'Coupons',
    component: () => import('@/views/Coupons.vue'),
    meta: { title: '优惠券' }
  },
  {
    path: '/merchant/:id',
    name: 'MerchantDetail',
    component: () => import('@/views/MerchantDetail.vue'),
    meta: { title: '商家详情' }
  },
  {
    path: '/my-orders',
    name: 'MyOrders',
    component: () => import('@/views/MyOrders.vue'),
    meta: { title: '我的优惠券', requireAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检测需要登录的页面
router.beforeEach((to, from, next) => {
  if (to.meta.requireAuth) {
    const token = localStorage.getItem('access_token')
    if (!token) {
      next('/login')
      return
    }
  }
  document.title = to.meta.title ? `XX营销平台 - ${to.meta.title}` : 'XX营销平台'
  next()
})

export default router
