const locationService = require('../../utils/location.js')

const STORAGE_KEYS = {
  LOCATION_HISTORY: 'locationHistory'
}

Page({
  data: {
    searchKeyword: '',
    searchResults: [],
    currentAddress: '',
    selectedLocation: null,
    locationLoading: false,
    searchLoading: false,
    debounceTimer: null,
    history: [],
    
    mapLatitude: 39.908823,
    mapLongitude: 116.397470,
    mapScale: 16,
    markers: []
  },

  onLoad(options) {
    wx.setNavigationBarTitle({
      title: '选择收货地址'
    })
    this.loadHistory()

    if (options.latitude && options.longitude) {
      const lat = parseFloat(options.latitude)
      const lng = parseFloat(options.longitude)
      this.setData({
        selectedLocation: { latitude: lat, longitude: lng },
        mapLatitude: lat,
        mapLongitude: lng
      })
      this.getAddressFromLocation(lat, lng)
    } else {
      this.getCurrentLocation()
    }
  },

  onShow() {
    if (!this.data.selectedLocation) {
      this.getCurrentLocation()
    }
  },

  onReady() {
    this.mapCtx = wx.createMapContext('locationMap')
  },

  async getCurrentLocation() {
    this.setData({ locationLoading: true })

    try {
      console.log('开始获取位置...')
      const location = await locationService.getCurrentLocation()
      console.log('获取位置成功:', location)
      
      this.setData({
        mapLatitude: location.latitude,
        mapLongitude: location.longitude,
        mapScale: 16
      })
      
      this.getAddressFromLocation(location.latitude, location.longitude)
    } catch (error) {
      console.error('获取位置失败:', error)
      this.setData({
        locationLoading: false,
        currentAddress: '定位失败，请检查权限或点击地图选择'
      })

      if (error.errMsg) {
        if (error.errMsg.includes('auth deny') || error.errMsg.includes('permission')) {
          this.showAuthDialog()
        }
      }
    }
  },

  async getAddressFromLocation(latitude, longitude) {
    try {
      console.log('开始逆地理编码:', latitude, longitude)
      const addressInfo = await locationService.reverseGeocoding(latitude, longitude)
      console.log('逆地理编码结果:', addressInfo)

      let fullAddress = ''
      if (addressInfo.province && addressInfo.city) {
        if (addressInfo.province === addressInfo.city) {
          fullAddress = `${addressInfo.province}${addressInfo.district}${addressInfo.street}${addressInfo.detailAddress ? ' ' + addressInfo.detailAddress : ''}`
        } else {
          fullAddress = `${addressInfo.province}${addressInfo.city}${addressInfo.district}${addressInfo.street}${addressInfo.detailAddress ? ' ' + addressInfo.detailAddress : ''}`
        }
      } else if (addressInfo.formattedAddress && addressInfo.formattedAddress.length > 5) {
        fullAddress = addressInfo.formattedAddress
      } else {
        fullAddress = `经度:${longitude.toFixed(4)}, 纬度:${latitude.toFixed(4)}`
      }

      this.setData({
        currentAddress: fullAddress || '无法识别地址',
        locationLoading: false,
        selectedLocation: {
          latitude,
          longitude,
          address: fullAddress,
          name: addressInfo.street || addressInfo.detailAddress || fullAddress
        }
      })
    } catch (error) {
      console.error('获取地址失败:', error)
      this.setData({
        locationLoading: false,
        currentAddress: `经度:${longitude.toFixed(4)}, 纬度:${latitude.toFixed(4)}`
      })
    }
  },

  openMapPicker() {
    wx.chooseLocation({
      latitude: this.data.mapLatitude,
      longitude: this.data.mapLongitude,
      success: (res) => {
        console.log('chooseLocation 结果:', res)
        if (res.name || res.address) {
          this.setData({
            mapLatitude: res.latitude,
            mapLongitude: res.longitude
          })
          this.getAddressFromLocation(res.latitude, res.longitude)
        }
      },
      fail: (err) => {
        console.log('chooseLocation 失败:', err)
      }
    })
  },

  onMapRegionChange(e) {
    if (e.type === 'end') {
      this.mapCtx.getCenterLocation({
        success: (res) => {
          this.getAddressFromLocation(res.latitude, res.longitude)
        }
      })
    }
  },

  onMarkerTap(e) {
    const markerId = e.detail.markerId
    const marker = this.data.markers.find(m => m.id === markerId)
    if (marker) {
      this.setData({
        selectedLocation: {
          latitude: marker.latitude,
          longitude: marker.longitude,
          address: marker.callout || marker.title,
          name: marker.title
        }
      })
    }
  },

  useCurrentLocation() {
    if (this.data.locationLoading) return
    this.getCurrentLocation()
  },

  showAuthDialog() {
    wx.showModal({
      title: '提示',
      content: '需要您的位置权限来定位，请到设置中开启',
      confirmText: '去设置',
      success: (res) => {
        if (res.confirm) {
          wx.openSetting()
        }
      }
    })
  },

  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({ searchKeyword: keyword })

    if (this.data.debounceTimer) {
      clearTimeout(this.data.debounceTimer)
    }

    if (keyword.length >= 2) {
      const timer = setTimeout(() => {
        this.onSearch()
      }, 300)
      this.setData({ debounceTimer: timer })
    } else if (keyword.length === 0) {
      this.setData({ searchResults: [] })
    }
  },

  async onSearch() {
    const keyword = this.data.searchKeyword.trim()
    if (!keyword) {
      this.setData({ searchResults: [] })
      return
    }

    this.setData({ searchLoading: true })

    try {
      let results = []
      if (this.data.selectedLocation) {
        const nearbyResults = await locationService.searchPlace(keyword)
        results = nearbyResults.map(item => {
          const loc = item.location || ''
          const parts = loc.split(',')
          return {
            id: item.id,
            name: item.name,
            address: item.address,
            latitude: parseFloat(parts[1]) || 0,
            longitude: parseFloat(parts[0]) || 0,
            distance: item._distance
          }
        })
      } else {
        const plainResults = await locationService.searchPlace(keyword)
        results = plainResults.map(item => {
          const loc = item.location || ''
          const parts = loc.split(',')
          return {
            id: item.id,
            name: item.name,
            address: item.address,
            latitude: parseFloat(parts[1]) || 0,
            longitude: parseFloat(parts[0]) || 0
          }
        })
      }

      this.setData({
        searchResults: results,
        searchLoading: false
      })
    } catch (error) {
      console.error('搜索失败:', error)
      this.setData({
        searchLoading: false,
        searchResults: []
      })
      wx.showToast({
        title: '搜索失败，请重试',
        icon: 'none'
      })
    }
  },

  clearSearch() {
    this.setData({
      searchKeyword: '',
      searchResults: []
    })
  },

  selectLocation(e) {
    const item = e.currentTarget.dataset.item
    const newLocation = {
      name: item.name,
      address: item.address,
      latitude: item.latitude,
      longitude: item.longitude,
      timestamp: Date.now()
    }

    this.setData({
      selectedLocation: newLocation,
      mapLatitude: item.latitude,
      mapLongitude: item.longitude,
      searchKeyword: '',
      searchResults: []
    })

    this.saveToHistory(newLocation)
  },

  selectQuickAddress(e) {
    const type = e.currentTarget.dataset.type
    wx.showToast({
      title: '请在地图上选择',
      icon: 'none'
    })
  },

  selectHistory(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      selectedLocation: {
        name: item.name,
        address: item.address || item.name,
        latitude: item.latitude,
        longitude: item.longitude,
        timestamp: Date.now()
      },
      mapLatitude: item.latitude,
      mapLongitude: item.longitude
    })
  },

  confirmLocation() {
    if (!this.data.selectedLocation) return

    const pages = getCurrentPages()
    const previousPage = pages[pages.length - 2]

    if (previousPage) {
      previousPage.setData({
        deliveryAddress: this.data.selectedLocation.address || this.data.selectedLocation.name,
        userLatitude: this.data.selectedLocation.latitude,
        userLongitude: this.data.selectedLocation.longitude
      })
    }

    wx.navigateBack()
  },

  goBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack({
        fail: (err) => {
          console.log('navigateBack 失败:', err)
          wx.switchTab({
            url: '/pages/index/index'
          })
        }
      })
    } else {
      wx.switchTab({
        url: '/pages/index/index'
      })
    }
  },

  loadHistory() {
    try {
      const history = wx.getStorageSync(STORAGE_KEYS.LOCATION_HISTORY) || '[]'
      const parsed = JSON.parse(history)
      this.setData({ history: parsed.slice(0, 10) })
    } catch (e) {
      this.setData({ history: [] })
    }
  },

  saveToHistory(location) {
    if (!location || !location.address) return

    let history = [...this.data.history]
    history = history.filter(item =>
      item.address !== location.address &&
      item.name !== location.name
    )

    history.unshift({
      name: location.name,
      address: location.address,
      latitude: location.latitude,
      longitude: location.longitude,
      timestamp: Date.now()
    })

    history = history.slice(0, 10)
    wx.setStorageSync(STORAGE_KEYS.LOCATION_HISTORY, JSON.stringify(history))
    this.setData({ history })
  },

  clearHistory() {
    wx.showModal({
      title: '清除历史',
      content: '确定清除所有历史记录？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync(STORAGE_KEYS.LOCATION_HISTORY)
          this.setData({ history: [] })
          wx.showToast({ title: '已清除', icon: 'success' })
        }
      }
    })
  },

  formatDistance(meters) {
    if (!meters) return ''
    if (meters < 1000) {
      return Math.round(meters) + 'm'
    }
    return (meters / 1000).toFixed(1) + 'km'
  },

  formatTime(timestamp) {
    if (!timestamp) return ''
    const date = new Date(timestamp)
    const now = new Date()
    const diff = now - date

    if (diff < 60000) {
      return '刚刚'
    } else if (diff < 3600000) {
      return Math.floor(diff / 60000) + '分钟前'
    } else if (diff < 86400000) {
      return Math.floor(diff / 3600000) + '小时前'
    } else {
      return date.toLocaleDateString()
    }
  }
})
