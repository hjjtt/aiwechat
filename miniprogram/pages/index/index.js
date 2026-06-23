const app = getApp()
const { address: addressAPI, menu } = require('../../utils/api.js')
const location = require('../../utils/location.js')

function getUserId() {
  const userId = wx.getStorageSync('userId')
  if (!userId) return null
  if (typeof userId === 'string' && userId.startsWith('user_')) {
    return parseInt(userId.replace('user_', ''), 10)
  }
  return parseInt(userId, 10)
}

// 根据分类返回emoji
function getCategoryEmoji(category) {
  const emojiMap = {
    '主食': '🍜',
    '热菜': '🍲',
    '素菜': '🥬',
    '汤类': '🍲',
    '凉菜': '🥗',
    '小吃': '🍟',
    '饮品': '☕',
    '甜品': '🍰'
  }
  return emojiMap[category] || '🍽️'
}

// 根据分类返回装饰标签
function getCategorySticker(category) {
  const stickerMap = {
    '主食': '🌾',
    '热菜': '🔥',
    '素菜': '🌿',
    '汤类': '♨️',
    '凉菜': '🥒',
    '小吃': '✨',
    '饮品': '☕',
    '甜品': '🍮'
  }
  return stickerMap[category] || '✨'
}

Page({
  data: {
    userAddress: null,
    hasAddress: false,
    currentLocation: null,
    locationAddress: '',
    isLocating: false,
    hotItems: [] // 热销推荐数据
  },

  onLoad() {
    // 页面加载时尝试获取当前位置
    this.getCurrentLocation()
    // 加载热销推荐
    this.loadHotItems()
  },

  onShow() {
    this.loadUserAddress()
  },

  // 加载热销推荐数据
  loadHotItems() {
    menu.getItems()
      .then(res => {
        const items = res.data || []
        // 按销量排序，取前4个
        const hotItems = items
          .filter(item => item.isAvailable)
          .sort((a, b) => (b.salesCount || 0) - (a.salesCount || 0))
          .slice(0, 4)
          .map((item, index) => {
            // 随机旋转角度
            const rotations = [-1.5, 0.8, -0.8, 1.2]
            return {
              ...item,
              priceFormatted: this.formatPrice(item.price),
              rotate: rotations[index] || 0,
              emoji: getCategoryEmoji(item.category),
              sticker: getCategorySticker(item.category)
            }
          })

        this.setData({ hotItems })
        console.log('热销推荐数据:', hotItems)
      })
      .catch(err => {
        console.error('加载热销数据失败:', err)
        // 失败时不显示默认数据，让 wxml 中的 wx:if 显示静态数据
        this.setData({ hotItems: [] })
      })
  },

  // 格式化价格
  formatPrice(price) {
    if (price === null || price === undefined || price === '') {
      return '0.00'
    }
    let num = 0
    if (typeof price === 'number') {
      num = price
    } else if (typeof price === 'string') {
      num = parseFloat(price)
    } else if (typeof price === 'object') {
      num = parseFloat(price.toString()) || 0
    }
    return num.toFixed(2)
  },

  // 获取当前位置
  async getCurrentLocation() {
    this.setData({ isLocating: true })
    try {
      const addressInfo = await location.getCurrentAddress()

      // 优化地址显示：只显示区 + 街道
      let displayAddress = ''
      if (addressInfo.district && addressInfo.street) {
        displayAddress = addressInfo.district + addressInfo.street
      } else if (addressInfo.city && addressInfo.district) {
        displayAddress = addressInfo.city + addressInfo.district
      } else if (addressInfo.formattedAddress) {
        displayAddress = addressInfo.formattedAddress
      } else {
        displayAddress = '配送范围内'
      }

      this.setData({
        currentLocation: addressInfo,
        locationAddress: displayAddress,
        isLocating: false
      })

      // 保存到全局供其他页面使用
      app.globalData = app.globalData || {}
      app.globalData.currentLocation = addressInfo
    } catch (error) {
      console.error('获取位置失败:', error)
      this.setData({
        isLocating: false,
        locationAddress: '配送范围内'
      })
    }
  },

  // 重新定位
  relocations() {
    this.getCurrentLocation()
  },

  // 选择位置
  async chooseLocation() {
    try {
      const result = await location.chooseLocation()
      if (result) {
        // 获取地址信息
        const addressInfo = await location.reverseGeocoding(result.latitude, result.longitude)

        // 优化地址显示：只显示区 + 街道
        let displayAddress = ''
        if (addressInfo.district && addressInfo.street) {
          displayAddress = addressInfo.district + addressInfo.street
        } else if (addressInfo.city && addressInfo.district) {
          displayAddress = addressInfo.city + addressInfo.district
        } else if (addressInfo.formattedAddress) {
          displayAddress = addressInfo.formattedAddress
        } else {
          displayAddress = result.name || result.address
        }

        this.setData({
          currentLocation: {
            latitude: result.latitude,
            longitude: result.longitude,
            ...addressInfo
          },
          locationAddress: displayAddress
        })

        // 保存到全局
        app.globalData = app.globalData || {}
        app.globalData.currentLocation = addressInfo
      }
    } catch (error) {
      console.error('选择位置失败:', error)
    }
  },

  loadUserAddress() {
    const userId = getUserId()
    if (!userId) {
      this.setData({
        userAddress: null,
        hasAddress: false
      })
      return
    }

    addressAPI.getAddresses(userId)
      .then(res => {
        const addresses = res.data || []
        if (addresses.length > 0) {
          const defaultAddr = addresses.find(a => a.isDefault) || addresses[0]
          this.setData({
            userAddress: defaultAddr,
            hasAddress: true
          })
        } else {
          this.setData({
            userAddress: null,
            hasAddress: false
          })
        }
      })
      .catch(err => {
        console.error('获取地址失败:', err)
        this.setData({
          userAddress: null,
          hasAddress: false
        })
      })
  },

  navigateToChat() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.navigateTo({
        url: '/pages/login/login'
      })
      return
    }
    wx.navigateTo({
      url: '/pages/chat/chat'
    })
  },

  // 跳转到菜单（可指定分类）
  navigateToMenu(e) {
    const category = e.currentTarget.dataset.category
    // 使用全局数据传递分类参数
    if (category) {
      app.globalData = app.globalData || {}
      app.globalData.targetCategory = category
    }
    wx.switchTab({
      url: '/pages/menu/menu'
    })
  },

  navigateToOrders() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.navigateTo({
        url: '/pages/login/login'
      })
      return
    }
    wx.switchTab({
      url: '/pages/orders/orders'
    })
  },

  navigateToAddressManage() {
    wx.navigateTo({
      url: '/pages/address-manage/address-manage'
    })
  },

  navigateToFavorites() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.navigateTo({
        url: '/pages/login/login'
      })
      return
    }
    wx.navigateTo({
      url: '/pages/favorites/favorites'
    })
  },

  navigateToProfile() {
    wx.switchTab({
      url: '/pages/profile/profile'
    })
  },

  // 查看全部热销
  viewAllHot() {
    wx.switchTab({
      url: '/pages/menu/menu'
    })
  },

  // 加入购物车
  addToCart(e) {
    const menuId = e.currentTarget.dataset.menuid
    const name = e.currentTarget.dataset.name
    const price = e.currentTarget.dataset.price

    // 获取当前购物车
    let cart = wx.getStorageSync('cart') || {}

    // 如果购物车是字符串，尝试解析
    if (typeof cart === 'string') {
      try {
        cart = JSON.parse(cart)
      } catch (err) {
        cart = {}
      }
    }

    // 确保 cart 是对象
    if (typeof cart !== 'object' || cart === null) {
      cart = {}
    }

    // 如果购物车中已有该商品，数量 +1
    if (cart[menuId]) {
      cart[menuId].quantity += 1
    } else {
      // 添加到购物车（使用 menu.js 期望的格式）
      cart[menuId] = {
        quantity: 1,
        item: {
          id: parseInt(menuId),
          name: name,
          price: parseFloat(price),
          priceFormatted: price + '.00'
        }
      }
    }

    // 保存购物车
    wx.setStorageSync('cart', JSON.stringify(cart))

    // 显示成功提示
    wx.showToast({
      title: '已加入购物车',
      icon: 'success',
      duration: 1500
    })

    // 通知购物车页面更新
    getApp().globalData.cartUpdated = true
  },

  // 跳转到菜单详情（暂时跳转到菜单页面）
  goToMenuDetail(e) {
    const name = e.currentTarget.dataset.name
    app.globalData = app.globalData || {}
    app.globalData.targetSearchKeyword = name

    wx.switchTab({
      url: '/pages/menu/menu'
    })
    // 提示用户
    wx.showToast({
      title: `正在查找：${name}`,
      icon: 'none',
      duration: 1500
    })
  },

  showHelp() {
    wx.showModal({
      title: '帮助中心',
      content: '1. 如需点餐，请进入"浏览菜单"选择菜品\n2. 如有疑问，可直接向AI客服提问\n3. 订单状态可在"我的订单"中查看',
      showCancel: false,
      confirmText: '知道了'
    })
  },
})
