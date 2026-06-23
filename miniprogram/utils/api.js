const { get, post, put, del } = require('./request')
const { BASE_URL } = require('./config')

const aiChat = {
  ask(question, userId, userContext = {}) {
    return post('/api/ai-chat/ask', { question, userId, ...userContext })
  },

  getHistory(userId) {
    return get('/api/ai-chat/history/' + encodeURIComponent(userId))
  },

  health() {
    return get('/api/admin/health')
  },

  uploadImage(filePath) {
    const app = getApp()
    const baseUrl = app.globalData.baseUrl || BASE_URL
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: `${baseUrl}/api/ai-chat/upload`,
        filePath,
        name: 'file',
        header: {
          Authorization: 'Bearer ' + wx.getStorageSync('token')
        },
        success: (res) => {
          if (res.statusCode === 200) {
            resolve(JSON.parse(res.data))
          } else {
            reject(res)
          }
        },
        fail: reject
      })
    })
  }
}

const knowledge = {
  search(query, topK = 5) {
    return get('/api/knowledge/search', { query, topK })
  }
}

const menu = {
  getItems() {
    return get('/api/menu/items')
  },

  getByCategory(category) {
    return get(`/api/menu/category/${encodeURIComponent(category)}`)
  },

  search(keyword) {
    return get('/api/menu/search', { keyword })
  },

  getItem(itemId) {
    return get('/api/menu/items/' + itemId)
  }
}

const order = {
  create(data) {
    const app = getApp()
    const baseUrl = app.globalData.baseUrl || BASE_URL
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${baseUrl}/api/orders`,
        method: 'POST',
        data,
        header: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer ' + wx.getStorageSync('token')
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
  },

  getDetail(id) {
    return get('/api/orders/id/' + id)
  },

  getByNumber(orderNumber) {
    return get('/api/orders/' + orderNumber)
  },

  getUserOrders(userId) {
    return get('/api/orders/user/' + userId)
  },

  getUserOrdersByStatus(userId, status) {
    return get('/api/orders/user/' + userId + '/status/' + status)
  },

  cancel(orderNumber, userId) {
    return post('/api/orders/' + orderNumber + '/cancel', null, { userId })
  },

  updateStatus(orderNumber, status) {
    return post('/api/orders/' + orderNumber + '/status', {}, { status })
  }
}

const wechat = {
  login(code, userInfo) {
    const app = getApp()
    const baseUrl = app.globalData.baseUrl || BASE_URL
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${baseUrl}/api/auth/login`,
        method: 'POST',
        data: { code, userInfo },
        header: {
          'Content-Type': 'application/json'
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
  },

  getUserInfo() {
    return get('/api/auth/validate')
  }
}

const favorite = {
  getFavorites(userId) {
    return get('/api/favorites/user/' + userId)
  },

  checkFavorite(userId, menuId) {
    return get('/api/favorites/check', { userId, menuId })
  },

  addFavorite(userId, menuId) {
    return post('/api/favorites', { userId, menuId })
  },

  removeFavorite(userId, menuId) {
    return del('/api/favorites', { userId, menuId })
  },

  toggleFavorite(userId, menuId) {
    return post('/api/favorites/toggle', { userId, menuId })
  },

  getFavoriteCount(userId) {
    return get('/api/favorites/count/' + userId)
  }
}

const address = {
  getAddresses(userId) {
    return get('/api/addresses/user/' + userId)
  },

  getDefaultAddress(userId) {
    return get('/api/addresses/user/' + userId + '/default')
  },

  getAddress(id) {
    return get('/api/addresses/' + id)
  },

  addAddress(data) {
    return post('/api/addresses', data)
  },

  updateAddress(id, data) {
    return put('/api/addresses/' + id, data)
  },

  deleteAddress(id) {
    return del('/api/addresses/' + id)
  },

  setDefault(id, userId) {
    return post('/api/addresses/' + id + '/default', { userId })
  }
}

const feedback = {
  submitFeedback(data) {
    const app = getApp()
    const baseUrl = app.globalData.baseUrl || BASE_URL
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${baseUrl}/api/feedback`,
        method: 'POST',
        data,
        header: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer ' + wx.getStorageSync('token')
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
  },

  getUserFeedback(userId) {
    return get('/api/feedback/user/' + userId)
  }
}

module.exports = {
  aiChat,
  knowledge,
  menu,
  order,
  wechat,
  favorite,
  address,
  feedback
}
