const { menu, order, favorite } = require('../../utils/api.js')
const app = getApp()

// 获取数字类型的 userId（兼容 "user_10" 或直接存储数字）
function getUserId() {
  const userId = wx.getStorageSync('userId')
  if (!userId) return null
  if (typeof userId === 'string' && userId.startsWith('user_')) {
    return parseInt(userId.replace('user_', ''), 10)
  }
  return parseInt(userId, 10)
}

Page({
  data: {
    items: [],
    categories: [],
    currentCategory: '',
    searchKeyword: '',
    cart: {},           // 购物车：{ menuId: { item, quantity } }
    cartArray: [],      // 购物车数组（用于 WXML 遍历）
    cartCount: 0,
    cartTotal: 0,
    showCartPopup: false
  },

  onLoad(options) {
    // 优先从 URL 参数获取搜索词或分类，其次从全局数据获取
    if (options && options.keyword) {
      this.setData({
        currentCategory: '',
        searchKeyword: decodeURIComponent(options.keyword)
      })
    } else if (app.globalData && app.globalData.targetSearchKeyword) {
      this.setData({
        currentCategory: '',
        searchKeyword: app.globalData.targetSearchKeyword
      })
      delete app.globalData.targetSearchKeyword
    } else if (options && options.category) {
      this.setData({ currentCategory: decodeURIComponent(options.category) })
    } else if (app.globalData && app.globalData.targetCategory) {
      this.setData({ currentCategory: app.globalData.targetCategory })
      // 不在这里删除 targetCategory，移到 onShow 中处理
    }
    this.loadCategories()
    // 先加载购物车，再加载菜品（loadItems 需要 cart 数据）
    this.loadCartFromStorage()
    if (this.data.searchKeyword) {
      this.doSearch()
    } else {
      this.loadItems()
    }
  },

  onShow() {
    // 每次显示页面时检查登录状态
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }
    // 刷新购物车数据（从结算页返回时可能已清空）
    this.loadCartFromStorage()

    // 检查是否从首页热销卡片跳转过来（switchTab 不触发 onLoad，所以在这里处理）
    if (app.globalData && app.globalData.targetSearchKeyword) {
      const keyword = app.globalData.targetSearchKeyword
      this.setData({
        currentCategory: '',
        searchKeyword: keyword
      })
      delete app.globalData.targetSearchKeyword
      this.doSearch()
      return
    }

    // 检查是否从首页分类卡片跳转过来（switchTab 不触发 onLoad，所以在这里处理）
    if (app.globalData && app.globalData.targetCategory) {
      const newCategory = app.globalData.targetCategory
      this.setData({
        currentCategory: newCategory,
        searchKeyword: ''
      })
      // 清除全局数据
      delete app.globalData.targetCategory
      // 重新加载对应分类的菜品
      this.loadItems()
    }
    // 同步刷新菜品列表中的数量显示
    this.syncCartToItems()
    // 刷新收藏状态
    this.loadFavoritesStatus()
  },

  // 加载分类列表
  loadCategories() {
    // 从菜品中提取分类
    menu.getItems()
      .then(res => {
        const items = res.data || []
        const categories = [...new Set(items.map(item => item.category))].filter(Boolean).sort()
        this.setData({ categories })
      })
      .catch(err => {
        console.error('加载分类失败:', err)
        // 使用默认分类
        this.setData({ categories: ['主食', '热菜', '素菜', '汤类'] })
      })
  },

  // 加载菜品列表
  loadItems() {
    const category = this.data.currentCategory

    if (category && category !== '' && category !== '全部') {
      // 按分类加载
      wx.showLoading({ title: '加载中...' })
      menu.getByCategory(category)
        .then(res => {
          const items = (res.data || []).map(item => ({
            ...item,
            priceFormatted: this.formatPrice(item.price),
            quantity: this.data.cart[item.id] ? this.data.cart[item.id].quantity : 0
          }))
          this.setData({ items })
          wx.hideLoading()
        })
        .catch(err => {
          console.error('加载菜品失败:', err)
          wx.hideLoading()
          wx.showToast({ title: '加载失败', icon: 'none' })
        })
    } else {
      // 加载全部
      wx.showLoading({ title: '加载中...' })
      menu.getItems()
        .then(res => {
          const items = res.data || []
          // 合并购物车数据并预处理价格格式
          const cart = this.data.cart
          items.forEach(item => {
            // 预处理价格格式
            item.priceFormatted = this.formatPrice(item.price)
            if (cart[item.id]) {
              item.quantity = cart[item.id].quantity
            } else {
              item.quantity = 0
            }
          })
          this.setData({ items })
          wx.hideLoading()
        })
        .catch(err => {
          console.error('加载菜品失败:', err)
          wx.hideLoading()
          wx.showToast({ title: '加载失败', icon: 'none' })
        })
    }
  },

  // 搜索输入
  onSearchInput(e) {
    this.setData({
      searchKeyword: e.detail.value
    })
  },

  // 执行搜索
  doSearch() {
    const keyword = this.data.searchKeyword.trim()
    if (!keyword) {
      this.loadItems()
      return Promise.resolve()
    }

    wx.showLoading({ title: '搜索中...' })
    return menu.search(keyword)
      .then(res => {
        const rawItems = this.filterItemsByKeyword(res.data || [], keyword)
        if (rawItems.length === 0) {
          wx.hideLoading()
          return this.searchItemsLocally(keyword)
        }

        const items = rawItems.map(item => ({
          ...item,
          priceFormatted: this.formatPrice(item.price),
          quantity: this.data.cart[item.id] ? this.data.cart[item.id].quantity : 0
        }))
        this.setData({ items })
        wx.hideLoading()
      })
      .catch(err => {
        console.error('搜索失败:', err)
        wx.hideLoading()
        return this.searchItemsLocally(keyword)
      })
  },

  // 本地关键词过滤，避免后端返回全量数据时搜索结果不生效
  filterItemsByKeyword(items, keyword) {
    const normalizedKeyword = String(keyword || '').trim().toLowerCase()
    if (!normalizedKeyword) return items

    return items.filter(item => {
      const searchableText = [
        item.name,
        item.description,
        item.category
      ].filter(Boolean).join(' ').toLowerCase()

      return searchableText.includes(normalizedKeyword)
    })
  },

  // 搜索接口失败时用全量菜品做本地兜底搜索
  searchItemsLocally(keyword) {
    return menu.getItems()
      .then(res => {
        const rawItems = this.filterItemsByKeyword(res.data || [], keyword)
        const items = rawItems.map(item => ({
          ...item,
          priceFormatted: this.formatPrice(item.price),
          quantity: this.data.cart[item.id] ? this.data.cart[item.id].quantity : 0
        }))
        this.setData({ items })
      })
      .catch(error => {
        console.error('本地搜索兜底失败:', error)
        wx.showToast({ title: '搜索失败', icon: 'none' })
      })
  },

  // 选择分类
  selectCategory(e) {
    const category = e.currentTarget.dataset.category

    if (category === '全部' || category === '') {
      this.setData({ currentCategory: '', searchKeyword: '' })
      this.loadItems()
      return
    }

    this.setData({ currentCategory: category, searchKeyword: '' })
    wx.showLoading({ title: '加载中...' })

    menu.getByCategory(category)
      .then(res => {
        const items = (res.data || []).map(item => ({
          ...item,
          priceFormatted: this.formatPrice(item.price),
          quantity: this.data.cart[item.id] ? this.data.cart[item.id].quantity : 0
        }))
        this.setData({ items })
        wx.hideLoading()
      })
      .catch(err => {
        console.error('加载菜品失败:', err)
        wx.hideLoading()
      })
  },

  // 增加数量
  increaseQuantity(e) {
    const item = e.currentTarget.dataset.item
    const cart = this.data.cart

    if (cart[item.id]) {
      cart[item.id].quantity++
    } else {
      cart[item.id] = { item: { ...item, quantity: 1 }, quantity: 1 }
    }

    this.updateCart(cart)
  },

  // 减少数量
  decreaseQuantity(e) {
    const item = e.currentTarget.dataset.item
    const cart = this.data.cart

    if (cart[item.id] && cart[item.id].quantity > 0) {
      cart[item.id].quantity--

      if (cart[item.id].quantity === 0) {
        delete cart[item.id]
      }
    }

    this.updateCart(cart)
  },

  // 更新购物车
  updateCart(cart) {
    // 将 cart 对象转换为数组（用于 WXML 遍历），并预处理价格
    const cartArray = Object.entries(cart).map(([menuId, value]) => ({
      menuId,
      ...value,
      item: {
        ...value.item,
        priceFormatted: this.formatPrice(value.item.price)
      }
    }))

    // 更新购物车中的数量显示
    const items = this.data.items.map(item => {
      if (cart[item.id]) {
        return { ...item, quantity: cart[item.id].quantity }
      }
      return { ...item, quantity: 0 }
    })

    // 计算总数
    let cartCount = 0
    let cartTotal = 0
    Object.values(cart).forEach(cartItem => {
      cartCount += cartItem.quantity
      cartTotal += cartItem.quantity * parseFloat(cartItem.item.price)
    })

    this.setData({
      cart,
      cartArray,
      items,
      cartCount,
      cartTotal: cartTotal.toFixed(2)
    })

    // 保存到本地存储
    this.saveCartToStorage(cart)
  },

  // 从本地存储加载购物车
  loadCartFromStorage() {
    try {
      let cartData = wx.getStorageSync('cart')
      
      // 如果购物车是字符串，尝试解析
      if (typeof cartData === 'string' && cartData) {
        try {
          cartData = JSON.parse(cartData)
        } catch (err) {
          cartData = {}
        }
      }
      
      // 确保 cart 是对象
      if (typeof cartData !== 'object' || cartData === null) {
        cartData = {}
      }
      
      if (Object.keys(cartData).length > 0) {
        // 将 cart 对象转换为数组，并预处理价格
        const cartArray = Object.entries(cartData).map(([menuId, value]) => {
          // 兼容旧格式：{ name, price, quantity }
          // 新格式：{ quantity, item: { name, price, priceFormatted } }
          const item = value.item || value
          return {
            menuId,
            quantity: value.quantity || 1,
            item: {
              ...item,
              priceFormatted: this.formatPrice(item.price)
            }
          }
        })
        
        // 计算总数
        let cartCount = 0
        let cartTotal = 0
        Object.values(cartData).forEach(cartItem => {
          const quantity = cartItem.quantity || 1
          const item = cartItem.item || cartItem
          cartCount += quantity
          cartTotal += quantity * parseFloat(item.price || 0)
        })
        
        this.setData({
          cart: cartData,
          cartArray,
          cartCount,
          cartTotal: cartTotal.toFixed(2)
        })
      } else {
        // 购物车为空时清空数据
        this.setData({
          cart: {},
          cartArray: [],
          cartCount: 0,
          cartTotal: 0
        })
      }
    } catch (e) {
      console.error('加载购物车失败:', e)
      // 出错时也清空
      this.setData({
        cart: {},
        cartArray: [],
        cartCount: 0,
        cartTotal: 0
      })
    }
  },

  // 同步购物车数据到菜品列表
  syncCartToItems() {
    const cart = this.data.cart
    const items = this.data.items.map(item => {
      if (cart[item.id]) {
        return { ...item, quantity: cart[item.id].quantity, priceFormatted: this.formatPrice(item.price) }
      }
      return { ...item, quantity: 0, priceFormatted: this.formatPrice(item.price) }
    })
    this.setData({ items })
  },

  // 保存购物车到本地存储
  saveCartToStorage(cart) {
    wx.setStorageSync('cart', JSON.stringify(cart))
  },

  // 清空购物车
  clearCart() {
    this.setData({
      cart: {},
      cartArray: [],
      cartCount: 0,
      cartTotal: 0,
      showCartPopup: false
    })
    wx.removeStorageSync('cart')

    // 重新加载菜品列表
    this.loadItems()
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

    // 跳转到结算页面
    wx.navigateTo({
      url: '/pages/checkout/checkout'
    })
  },

  // 格式化价格
  formatPrice(price) {
    if (price === null || price === undefined || price === '') {
      return '0.00'
    }
    // 处理 BigDecimal 或数字或字符串
    let num = 0
    if (typeof price === 'number') {
      num = price
    } else if (typeof price === 'string') {
      num = parseFloat(price)
    } else if (typeof price === 'object') {
      // 处理 BigDecimal 等对象类型
      num = parseFloat(price.toString()) || 0
    }
    return num.toFixed(2)
  },

  // 加载用户收藏状态
  loadFavoritesStatus() {
    const userId = getUserId()
    if (!userId) return

    const items = this.data.items
    items.forEach(item => {
      favorite.checkFavorite(userId, item.id)
        .then(res => {
          const isFavorite = res.data?.isFavorite || false
          // 只在状态改变时更新
          if (item.isFavorite !== isFavorite) {
            const updatedItems = this.data.items.map(i => {
              if (i.id === item.id) {
                return { ...i, isFavorite }
              }
              return i
            })
            this.setData({ items: updatedItems })
          }
        })
        .catch(err => {
          console.error('检查收藏状态失败:', err)
        })
    })
  },

  // 切换收藏状态
  toggleFavorite(e) {
    const item = e.currentTarget.dataset.item
    const userId = getUserId()

    if (!userId) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    const isFavorite = item.isFavorite || false
    const actionText = isFavorite ? '取消收藏' : '收藏'

    favorite.toggleFavorite(userId, item.id)
      .then(res => {
        const result = res.data || {}
        const newIsFavorite = result.isFavorite !== undefined ? result.isFavorite : !isFavorite

        // 更新当前菜品的状态
        const updatedItems = this.data.items.map(i => {
          if (i.id === item.id) {
            return { ...i, isFavorite: newIsFavorite }
          }
          return i
        })
        this.setData({ items: updatedItems })

        wx.showToast({
          title: result.message || `${actionText}成功`,
          icon: 'success'
        })
      })
      .catch(err => {
        console.error('操作失败:', err)
        wx.showToast({ title: '操作失败', icon: 'none' })
      })
  }
})
