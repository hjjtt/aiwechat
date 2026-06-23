const { BASE_URL } = require('./config')

let isRefreshing = false
let pendingRequests = []

function request(url, method = 'GET', data = {}) {
  const header = {
    'Content-Type': 'application/json'
  }
  const token = wx.getStorageSync('token')
  if (token) {
    header.Authorization = `Bearer ${token}`
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + url,
      method,
      data,
      header,
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else if (res.statusCode === 401) {
          // 将当前请求加入等待队列，等刷新完自动重试
          pendingRequests.push({ url, method, data, resolve, reject })
          silentRelogin()
        } else {
          reject(res)
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

function silentRelogin() {
  if (isRefreshing) return
  isRefreshing = true

  wx.showLoading({ title: '重新登录中...', mask: true })

  const nickname = wx.getStorageSync('nickname')

  // 仅使用 wx.login 真实登录
  wx.login({
    success: (loginRes) => {
      if (loginRes.code) {
        doRelogin({ code: loginRes.code, userInfo: { nickName: nickname } }, '/api/auth/login')
      } else {
        reloginFailed()
      }
    },
    fail: () => {
      reloginFailed()
    }
  })
}

function doRelogin(body, endpoint) {
  wx.request({
    url: BASE_URL + endpoint,
    method: 'POST',
    header: { 'Content-Type': 'application/json' },
    data: body,
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.success && res.data.data) {
        const loginData = res.data.data
        wx.setStorageSync('token', loginData.token)
        wx.setStorageSync('userId', loginData.userId)
        wx.setStorageSync('nickname', loginData.nickname)
        wx.setStorageSync('avatarUrl', loginData.avatarUrl)

        wx.hideLoading()
        retryPendingRequests(loginData.token)
      } else {
        reloginFailed()
      }
    },
    fail: () => {
      reloginFailed()
    },
    complete: () => {
      isRefreshing = false
      wx.hideLoading()
    }
  })
}

function retryPendingRequests(newToken) {
  const requests = pendingRequests.slice()
  pendingRequests = []

  requests.forEach(({ url, method, data, resolve, reject }) => {
    wx.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${newToken}`
      },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(res)
        }
      },
      fail: reject
    })
  })
}

function reloginFailed() {
  pendingRequests = []
  wx.hideLoading()
  wx.removeStorageSync('token')
  wx.removeStorageSync('userId')
  wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
  setTimeout(() => {
    wx.reLaunch({ url: '/pages/login/login' })
  }, 1500)
}

function get(url, params = {}) {
  let queryString = ''
  if (Object.keys(params).length > 0) {
    queryString = '?' + Object.keys(params)
      .map(key => `${key}=${encodeURIComponent(params[key] || '')}`)
      .join('&')
  }
  return request(url + queryString, 'GET')
}

function post(url, data = {}, params = {}) {
  let queryString = ''
  if (Object.keys(params).length > 0) {
    queryString = '?' + Object.keys(params)
      .map(key => `${key}=${encodeURIComponent(params[key] || '')}`)
      .join('&')
  }
  return request(url + queryString, 'POST', data)
}

function put(url, data = {}) {
  return request(url, 'PUT', data)
}

function del(url, data = {}) {
  return request(url, 'DELETE', data)
}

module.exports = {
  BASE_URL,
  request,
  get,
  post,
  put,
  del
}
