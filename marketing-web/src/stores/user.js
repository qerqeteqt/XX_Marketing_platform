import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)

  const isLoggedIn = computed(() => !!user.value)

  const nickname = computed(() => user.value?.nickname || '')

  function setUser(u) {
    user.value = u
  }

  function logout() {
    user.value = null
  }

  return { user, isLoggedIn, nickname, setUser, logout }
})
