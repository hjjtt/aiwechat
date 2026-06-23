import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, logout, getUserInfo } from '@/api/auth'
import router from '@/router'

function readStoredUser() {
  const raw = localStorage.getItem('admin_user')
  if (!raw || raw === 'undefined' || raw === 'null') {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    console.warn('Failed to parse admin_user from localStorage, clearing invalid value.', error)
    localStorage.removeItem('admin_user')
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userInfo = ref(readStoredUser())
  const isLoggedIn = computed(() => !!token.value)

  async function doLogin(username, password) {
    try {
      const response = await login(username, password)
      if (response.data.success) {
        const loginData = response.data.data || {}
        token.value = loginData.token || ''
        userInfo.value = loginData.user || {
          username: loginData.username || username,
          nickname: loginData.username || username
        }

        localStorage.setItem('admin_token', token.value)
        localStorage.setItem('admin_user', JSON.stringify(userInfo.value))

        return { success: true }
      }
      return { success: false, message: response.data.message || '登录失败' }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '登录失败' }
    }
  }

  async function doLogout() {
    try {
      await logout()
    } catch (e) {
      console.error('退出登录失败', e)
    }

    token.value = ''
    userInfo.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')

    router.push({ name: 'Login' })
  }

  async function fetchUserInfo() {
    if (!token.value) return

    try {
      const response = await getUserInfo()
      if (response.data.success) {
        userInfo.value = response.data.data
        localStorage.setItem('admin_user', JSON.stringify(userInfo.value))
      }
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    doLogin,
    doLogout,
    fetchUserInfo
  }
})
