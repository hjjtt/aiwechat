const { favorite } = require('../../utils/api.js')
const { menu } = require('../../utils/api.js')

// 获取数字类型的 userId（兼容 "user_10" 或直接存储数字）
function getUserId() {
  const userId = wx.getStorageSync('userId')
  if (!userId) return null
  // 如果是 "user_10" 格式，提取数字部分
  if (typeof userId === 'string' && userId.startsWith('user_')) {
    return parseInt(userId.replace('user_', ''), 10)
  }
  return parseInt(userId, 10)
}

Page({
  data: {
    favorites: [],
    cart: {},
    cartCount: 0,
    cartTotal: 0,
    showCartPopup: false,
    isLoggedIn: false
  },

  onLoad() {
    this.checkLogin()
  },

  onShow() {
    this.checkLogin()
  },

  // 检查登录状态
  checkLogin() {
    const token = wx.getStorageSync('token')
    const rawUserId = wx.getStorageSync('userId')

    if (!token || !rawUserId) {
      this.setData({ isLoggedIn: false })
      wx.showModal({
        title: '提示',
        content: '请先登录后查看收藏',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({
              url: '/pages/login/login'
            })
          }
        }
      })
      return
    }

    const userId = getUserId()
    this.setData({ isLoggedIn: true, userId })
    this.loadFavorites()
    this.loadCartFromStorage()
  },

  // 加载收藏列表
  loadFavorites() {
    const userId = getUserId()
    if (!userId) return

    wx.showLoading({ title: '加载中...' })

    favorite.getFavorites(userId)
      .then(res => {
        const favorites = res.data || []
        // 预处理价格格式
        favorites.forEach(item => {
          item.priceFormatted = this.formatPrice(item.menuPrice || item.price)
          item.quantity = 0
        })
        this.setData({ favorites })
        wx.hideLoading()
      })
      .catch(err => {
        console.error('加载收藏失败:', err)
        wx.hideLoading()
        wx.showToast({ title: '加载失败', icon: 'none' })
      })
  },

  // 切换收藏状态（取消收藏）
  toggleFavorite(e) {
    const item = e.currentTarget.dataset.item
    const userId = getUserId()
    if (!userId) return

    wx.showModal({
      title: '取消收藏',
      content: `确定取消收藏「${item.menuName}」吗？`,
      success: (res) => {
        if (res.confirm) {
          favorite.removeFavorite(userId, item.menuId)
            .then(() => {
              wx.showToast({ title: '已取消收藏', icon: 'success' })
              // 从列表中移除
              const favorites = this.data.favorites.filter(f => f.menuId !== item.menuId)
              this.setData({ favorites })
            })
            .catch(err => {
              console.error('取消收藏失败:', err)
              wx.showToast({ title: '操作失败', icon: 'none' })
            })
        }
      }
    })
  },

  // 增加数量
  increaseQuantity(e) {
    const item = e.currentTarget.dataset.item
    const cart = this.data.cart

    const menuId = item.menuId || item.id
    if (cart[menuId]) {
      cart[menuId].quantity++
    } else {
      cart[menuId] = {
        item: {
          id: menuId,
          name: item.menuName || item.name,
          price: item.menuPrice || item.price,
          priceFormatted: this.formatPrice(item.menuPrice || item.price),
          category: item.menuCategory || item.category
        },
        quantity: 1
      }
    }

    this.updateCart(cart)
  },

  // 减少数量
  decreaseQuantity(e) {
    const item = e.currentTarget.dataset.item
    const cart = this.data.cart

    const menuId = item.menuId || item.id
    if (cart[menuId] && cart[menuId].quantity > 0) {
      cart[menuId].quantity--

      if (cart[menuId].quantity === 0) {
        delete cart[menuId]
      }
    }

    this.updateCart(cart)
  },

  // 更新购物车
  updateCart(cart) {
    // 同步更新收藏列表中的数量
    const favorites = this.data.favorites.map(fav => {
      const menuId = fav.menuId || fav.id
      if (cart[menuId]) {
        return { ...fav, quantity: cart[menuId].quantity, priceFormatted: this.formatPrice(fav.menuPrice || fav.price) }
      }
      return { ...fav, quantity: 0, priceFormatted: this.formatPrice(fav.menuPrice || fav.price) }
    })

    // 计算总数
    let cartCount = 0
    let cartTotal = 0
    Object.values(cart).forEach(cartItem => {
      cartCount += cartItem.quantity
      cartTotal += cartItem.quantity * parseFloat(cartItem.item.price || 0)
    })

    this.setData({
      cart,
      favorites,
      cartCount,
      cartTotal: cartTotal.toFixed(2)
    })

    // 保存到本地存储
    wx.setStorageSync('cart', JSON.stringify(cart))
  },

  // 从本地存储加载购物车
  loadCartFromStorage() {
    try {
      const cartStr = wx.getStorageSync('cart')
      if (cartStr) {
        const cart = JSON.parse(cartStr)
        let cartCount = 0
        let cartTotal = 0
        Object.values(cart).forEach(cartItem => {
          cartCount += cartItem.quantity
          cartTotal += cartItem.quantity * parseFloat(cartItem.item.price || 0)
        })
        this.setData({
          cart,
          cartCount,
          cartTotal: cartTotal.toFixed(2)
        })
      }
    } catch (e) {
      console.error('加载购物车失败:', e)
    }
  },

  // 去结算
  checkout() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.navigateTo({
        url: '/pages/login/login'
      })
      return
    }

    if (this.data.cartCount === 0) {
      wx.showToast({
        title: '请先添加菜品',
        icon: 'none'
      })
      return
    }

    wx.navigateTo({
      url: '/pages/checkout/checkout'
    })
  },

  // 显示购物车弹窗
  showCartPopup() {
    if (this.data.cartCount === 0) {
      return
    }
    this.setData({ showCartPopup: true })
  },

  // 关闭购物车弹窗
  closeCartPopup() {
    this.setData({ showCartPopup: false })
  },

  // 清空购物车
  clearCart() {
    this.setData({
      cart: {},
      cartCount: 0,
      cartTotal: 0,
      showCartPopup: false
    })
    wx.removeStorageSync('cart')
    // 同步更新收藏列表
    const favorites = this.data.favorites.map(fav => ({ ...fav, quantity: 0 }))
    this.setData({ favorites })
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

  // 返回首页
  goHome() {
    wx.switchTab({
      url: '/pages/index/index'
    })
  }
})
