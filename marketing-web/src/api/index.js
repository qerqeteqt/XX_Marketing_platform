import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器 — 自动带 Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const refreshToken = localStorage.getItem('refresh_token')
  if (refreshToken) {
    config.headers['X-Refresh-Token'] = refreshToken
  }
  return config
})

// 响应拦截器 — 无感刷新
request.interceptors.response.use(
  response => {
    // 检查响应头中是否有新的 Access Token
    const newToken = response.headers['x-new-access-token']
    if (newToken) {
      localStorage.setItem('access_token', newToken)
    }
    return response.data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      window.location.href = '/login'
    }
    ElMessage.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)

export default request

// API 接口
export const userApi = {
  sendCode: (phone) => request.post('/user/send-code', null, { params: { phone } }),
  login: (data) => request.post('/user/login', data),
  register: (data) => request.post('/user/register', data),
  refresh: (refreshToken) => request.post('/user/refresh', null, { params: { refreshToken } }),
  logout: (refreshToken) => request.post('/user/logout', null, { params: { refreshToken } }),
  getMe: () => request.get('/user/me'),
  follow: (userId) => request.post(`/user/follow/${userId}`),
  unfollow: (userId) => request.delete(`/user/follow/${userId}`),
  getFollowing: () => request.get('/user/following'),
  isFollowing: (userId) => request.get(`/user/is-following/${userId}`),
}

export const couponApi = {
  list: () => request.get('/coupon/list'),
  listByMerchant: (merchantId) => request.get(`/coupon/merchant/${merchantId}`),
  detail: (id) => request.get(`/coupon/detail/${id}`),
  grab: (activityId) => request.post('/coupon/grab', { activityId }),
  stock: (id) => request.get(`/coupon/stock/${id}`),
}

export const orderApi = {
  myOrders: () => request.get('/order/my'),
  useCoupon: (orderId) => request.post(`/order/use/${orderId}`),
}

export const merchantApi = {
  hot: () => request.get('/merchant/hot'),
  detail: (id) => request.get(`/merchant/detail/${id}`),
}
