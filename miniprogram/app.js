const API = require('./utils/api')
const { BASE_URL } = require('./utils/config')

App({
  onLaunch() {
    this.checkLoginStatus()
  },

  onShow() {
    this.checkLoginStatus()
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token')
    if (!token) {
      const pages = getCurrentPages()
      if (pages.length > 0 && pages[pages.length - 1].route !== 'pages/login/login') {
        wx.redirectTo({
          url: '/pages/login/login'
        })
      }
      return false
    }

    return true
  },

  isLoggedIn() {
    const token = wx.getStorageSync('token')
    return !!token
  },

  getToken() {
    return wx.getStorageSync('token') || ''
  },

  logout() {
    const baseUrl = this.globalData.baseUrl
    const token = this.getToken()

    if (token) {
      wx.request({
        url: `${baseUrl}/api/auth/logout`,
        method: 'POST',
        header: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        complete: () => {
          this.clearUserData()
        }
      })
    } else {
      this.clearUserData()
    }
  },

  clearUserData() {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userId')
    wx.removeStorageSync('nickname')
    wx.removeStorageSync('avatarUrl')
    wx.removeStorageSync('isNewUser')

    wx.reLaunch({
      url: '/pages/login/login'
    })
  },

  globalData: {
    userInfo: null,
    baseUrl: BASE_URL
  }
})
