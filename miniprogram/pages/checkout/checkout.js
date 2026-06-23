const { order, address: addressAPI } = require("../../utils/api.js");

// 地址历史记录最大保存数量
const MAX_HISTORY_COUNT = 5;

Page({
  data: {
    cart: {},
    cartItems: [],
    totalAmount: 0,
    userId: "",
    contactName: "",
    contactPhone: "",
    deliveryAddress: "",
    detailAddress: "", // 详细地址（楼栋门牌等）
    remark: "",
    submitting: false,
    checkedItems: {}, // 选中的商品
    selectAll: true, // 全选状态
    userLatitude: "", // 用户位置纬度
    userLongitude: "", // 用户位置经度
    addressLoading: false, // 地址加载状态
    addressHistory: [], // 历史地址列表
    savedAddresses: [], // 保存的地址列表
    currentAddressIndex: 0, // 当前选中的地址索引
  },

  onLoad() {
    this.loadCart();
    this.loadUserInfo();
    this.loadSavedAddresses();
  },

  // 加载用户保存的地址
  loadSavedAddresses() {
    let userId = this.data.userId;
    if (!userId) return;

    // 处理 userId 格式，可能是 "user_10" 或纯数字
    if (typeof userId === "string" && userId.startsWith("user_")) {
      userId = userId.replace("user_", "");
    }
    userId = parseInt(userId) || 0;
    if (!userId) return;

    addressAPI
      .getAddresses(userId)
      .then((res) => {
        const addresses = res.data || [];
        if (addresses.length > 0) {
          const defaultAddr =
            addresses.find((a) => a.isDefault) || addresses[0];
          this.setData({
            deliveryAddress: defaultAddr.address,
            contactName: defaultAddr.contactName || this.data.contactName,
            contactPhone: defaultAddr.contactPhone || this.data.contactPhone,
            userLatitude: defaultAddr.latitude,
            userLongitude: defaultAddr.longitude,
            savedAddresses: addresses,
            currentAddressIndex: addresses.findIndex(a => a.id === defaultAddr.id),
          });
        } else {
          this.setData({ savedAddresses: [] });
        }
      })
      .catch((err) => {
        console.error("加载地址失败:", err);
        this.setData({ savedAddresses: [] });
      });
  },

  onShow() {
    // 检查登录状态
    const token = wx.getStorageSync("token");
    if (!token) {
      wx.redirectTo({
        url: "/pages/login/login",
      });
    }
    // 每次显示时刷新购物车和地址
    this.loadCart();
    this.loadSavedAddresses();
  },

  // 选择保存的地址
  selectSavedAddress(e) {
    const index = e.currentTarget.dataset.index;
    const addr = this.data.savedAddresses[index];
    if (addr) {
      this.setData({
        currentAddressIndex: index,
        deliveryAddress: addr.address,
        contactName: addr.contactName || this.data.contactName,
        contactPhone: addr.contactPhone || this.data.contactPhone,
        userLatitude: addr.latitude,
        userLongitude: addr.longitude,
        detailAddress: addr.detailAddress || "",
      });
    }
  },

  // 加载购物车数据
  loadCart() {
    try {
      let cartData = wx.getStorageSync("cart")

      console.log('原始购物车数据:', cartData)

      // 如果购物车是字符串，尝试解析
      if (typeof cartData === 'string' && cartData) {
        try {
          cartData = JSON.parse(cartData)
        } catch (err) {
          console.error('JSON 解析失败:', err)
          cartData = {}
        }
      }

      // 确保 cart 是对象
      if (typeof cartData !== 'object' || cartData === null) {
        cartData = {}
      }

      console.log('解析后的购物车:', cartData)

      if (cartData && Object.keys(cartData).length > 0) {
        // 将 cart 对象转换为数组，添加 menuId
        const cartItems = Object.entries(cartData).map(([key, value]) => {
          console.log('购物车项 key:', key, 'value:', value)

          // 修复：如果 key 不是数字（损坏的数据），尝试从 value.item.id 获取
          let menuId = key
          let itemData = value

          if (isNaN(parseInt(key))) {
            // key 不是数字ID，可能是名称，检查 value 结构
            if (value && value.item && value.item.id) {
              // 从 item 中获取正确的 ID
              menuId = String(value.item.id)
              console.log('修复损坏的购物车数据: key从', key, '改为', menuId)
            } else if (typeof value === 'object' && value.name) {
              // 旧格式数据：{ name, price, quantity }
              // 这种数据无法修复，需要提示用户清除
              console.error('检测到旧格式购物车数据，无法自动修复')
              wx.showModal({
                title: '购物车数据异常',
                content: '检测到旧版购物车数据，请清除后重新添加商品',
                showCancel: false,
                success: () => {
                  wx.removeStorageSync('cart')
                  this.setData({
                    cart: {},
                    cartItems: [],
                    totalAmount: 0,
                    checkedItems: {},
                  })
                }
              })
              return null
            }
          }

          // 确保 itemData 有正确的结构
          if (!itemData.item && itemData.name) {
            // 兼容旧格式：{ name, price, quantity }
            itemData = { item: { ...itemData }, quantity: itemData.quantity || 1 }
          }

          return {
            menuId,
            quantity: itemData.quantity || 1,
            item: itemData.item || itemData,
          }
        }).filter(item => item !== null) // 过滤掉无效项

        // 计算总价
        let totalAmount = 0
        const checkedItems = {}

        cartItems.forEach((item, index) => {
          const itemData = item.item || {}
          const price = parseFloat(itemData.price || 0)
          const quantity = item.quantity || 1
          const subtotal = price * quantity
          totalAmount += subtotal
          // 使用 menuId 作为 checkedItems 的 key
          checkedItems[item.menuId] = true
        })

        console.log('购物车数组:', cartItems)
        console.log('选中项:', checkedItems)
        console.log('总价:', totalAmount)

        // 如果有修复的数据，重新保存购物车
        const correctedCart = {}
        cartItems.forEach(item => {
          correctedCart[item.menuId] = {
            quantity: item.quantity,
            item: item.item
          }
        })
        wx.setStorageSync('cart', JSON.stringify(correctedCart))

        this.setData({
          cart: correctedCart,
          cartItems,
          totalAmount: totalAmount.toFixed(2),
          checkedItems,
        })
      } else {
        // 购物车为空
        console.log('购物车为空')
        this.setData({
          cart: {},
          cartItems: [],
          totalAmount: 0,
          checkedItems: {},
        })
      }
    } catch (e) {
      console.error("加载购物车失败:", e)
      // 出错时清空
      this.setData({
        cart: {},
        cartItems: [],
        totalAmount: 0,
        checkedItems: {},
      })
    }
  },

  // 加载用户信息
  loadUserInfo() {
    let userId = wx.getStorageSync("userId") || "";
    const nickname = wx.getStorageSync("nickname") || "";
    const avatarUrl = wx.getStorageSync("avatarUrl") || "";

    this.setData({
      userId,
      contactName: nickname,
    });
  },

  // 加载历史地址
  loadAddressHistory() {
    try {
      const history = wx.getStorageSync("addressHistory") || "[]";
      const addressHistory = JSON.parse(history);
      this.setData({ addressHistory });
    } catch (e) {
      this.setData({ addressHistory: [] });
    }
  },

  // 保存地址到历史记录
  saveAddressToHistory(address, latitude, longitude) {
    if (!address) return;

    let history = [...this.data.addressHistory];

    // 移除已存在的相同地址
    history = history.filter((item) => item.address !== address);

    // 添加到开头
    history.unshift({
      address,
      latitude,
      longitude,
      timestamp: Date.now(),
    });

    // 只保留最近的 MAX_HISTORY_COUNT 条
    if (history.length > MAX_HISTORY_COUNT) {
      history = history.slice(0, MAX_HISTORY_COUNT);
    }

    // 保存到本地存储
    wx.setStorageSync("addressHistory", JSON.stringify(history));

    this.setData({ addressHistory: history });
  },

  // 切换商品选中状态
  toggleItem(e) {
    const itemId = e.currentTarget.dataset.id;
    const checkedItems = { ...this.data.checkedItems };
    checkedItems[itemId] = !checkedItems[itemId];

    // 检查是否全选
    let selectAll = true;
    this.data.cartItems.forEach((item) => {
      if (!checkedItems[item.menuId]) {
        selectAll = false;
      }
    });

    this.setData({ checkedItems, selectAll });
    this.calculateTotal();
  },

  // 全选/取消全选
  toggleSelectAll() {
    const selectAll = !this.data.selectAll;
    const checkedItems = {};

    this.data.cartItems.forEach((item) => {
      checkedItems[item.menuId] = selectAll;
    });

    this.setData({ checkedItems, selectAll });
    this.calculateTotal();
  },

  // 计算总价
  calculateTotal() {
    let totalAmount = 0;

    this.data.cartItems.forEach((item) => {
      if (this.data.checkedItems[item.menuId]) {
        totalAmount += item.quantity * parseFloat(item.item.price || 0);
      }
    });

    this.setData({ totalAmount: totalAmount.toFixed(2) });
  },

  // 输入处理
  onNameInput(e) {
    this.setData({ contactName: e.detail.value });
  },

  onPhoneInput(e) {
    this.setData({ contactPhone: e.detail.value });
  },

  onAddressInput(e) {
    this.setData({ deliveryAddress: e.detail.value });
  },

  onDetailAddressInput(e) {
    this.setData({ detailAddress: e.detail.value });
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value });
  },

  // 提交订单
  submitOrder() {
    // 验证 - 必须选择地址
    if (!this.data.deliveryAddress) {
      wx.showToast({ title: "请选择配送地址", icon: "none" });
      return;
    }

    // 如果没有填写联系信息，从已选地址中获取
    let contactName = this.data.contactName;
    let contactPhone = this.data.contactPhone;

    // 如果 data 中没有联系信息，尝试从已选地址中获取
    if (!contactName || !contactPhone) {
      const selectedAddr = this.data.savedAddresses?.find(
        (addr) => addr.address === this.data.deliveryAddress
      );
      if (selectedAddr) {
        contactName = contactName || selectedAddr.contactName;
        contactPhone = contactPhone || selectedAddr.contactPhone;
      }
    }

    // 再次验证
    if (!contactName || !contactName.trim()) {
      wx.showToast({ title: "请填写联系人", icon: "none" });
      return;
    }

    if (!contactPhone || !contactPhone.trim()) {
      wx.showToast({ title: "请填写联系电话", icon: "none" });
      return;
    }

    // 构建完整地址
    const fullAddress = this.data.detailAddress
      ? `${this.data.deliveryAddress} ${this.data.detailAddress}`
      : this.data.deliveryAddress;

    // 获取选中的商品
    const selectedItems = this.data.cartItems.filter(
      (item) => this.data.checkedItems[item.menuId]
    );

    if (selectedItems.length === 0) {
      wx.showToast({ title: "请选择商品", icon: "none" });
      return;
    }

    // 验证商品数据有效性
    for (const item of selectedItems) {
      const menuId = parseInt(item.menuId);
      if (isNaN(menuId) || menuId <= 0) {
        console.error("无效的菜单ID:", item.menuId);
        wx.showToast({ title: "购物车数据异常，请重新添加商品", icon: "none" });
        return;
      }
      if (!item.quantity || item.quantity <= 0) {
        console.error("无效的数量:", item.quantity);
        wx.showToast({ title: "商品数量异常，请重新添加商品", icon: "none" });
        return;
      }
      if (!item.item || !item.item.price) {
        console.error("商品价格缺失:", item);
        wx.showToast({ title: "商品价格异常，请重新添加商品", icon: "none" });
        return;
      }
    }

    if (this.data.submitting) {
      return;
    }

    this.setData({ submitting: true });

    // 构建订单项
    const items = selectedItems.map((cartItem) => ({
      menuId: parseInt(cartItem.menuId),
      name: cartItem.item.name,
      price: parseFloat(cartItem.item.price || 0),
      quantity: cartItem.quantity,
    }));

    // 处理 userId
    let userIdStr = this.data.userId;
    if (typeof userIdStr === "string" && userIdStr.startsWith("user_")) {
      userIdStr = userIdStr.substring(5);
    }
    const userIdNum = parseInt(userIdStr) || 0;

    if (!userIdNum || userIdNum <= 0) {
      wx.showToast({ title: "用户未登录，请重新登录", icon: "none" });
      return;
    }

    console.log('提交订单数据:', {
      userId: userIdNum,
      items: items,
      totalAmount: this.data.totalAmount
    })

    order
      .create({
        userId: userIdNum,
        contactName: contactName.trim(),
        contactPhone: contactPhone.trim(),
        deliveryAddress: fullAddress,
        remark: this.data.remark.trim(),
        totalAmount: parseFloat(this.data.totalAmount),
        items: items,
      })
      .then((res) => {
        wx.hideLoading();

        if (res && res.success) {
          // 订单创建成功
          wx.showToast({ title: "订单创建成功", icon: "success" });

          // 保存地址到历史记录
          this.saveAddressToHistory(
            fullAddress,
            this.data.userLatitude,
            this.data.userLongitude
          );

          // 清空整个购物车
          wx.removeStorageSync("cart");

          // 跳转到订单列表
          setTimeout(() => {
            wx.switchTab({
              url: "/pages/orders/orders",
            });
          }, 1500);
        } else {
          wx.showToast({
            title: res?.message || "创建订单失败",
            icon: "none",
          });
        }
      })
      .catch((err) => {
        wx.hideLoading();
        console.error("创建订单失败:", err);
        // 尝试获取后端返回的错误信息
        let errorMsg = "创建订单失败";
        if (err && err.data) {
          // 后端返回 ApiResponse.error() 时，错误信息在 error 字段
          if (err.data.error) {
            errorMsg = err.data.error;
          } else if (err.data.message) {
            errorMsg = err.data.message;
          } else if (typeof err.data === 'string') {
            errorMsg = err.data;
          }
        } else if (err && err.errMsg) {
          errorMsg = err.errMsg;
        }
        wx.showToast({
          title: errorMsg,
          icon: "none",
        });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },

  // 格式化价格
  formatPrice(price) {
    return parseFloat(price || 0).toFixed(2);
  },

  /**
   * 选择配送地址 - 跳转到地址管理页面
   */
  chooseAddress() {
    wx.navigateTo({
      url: "/pages/address-manage/address-manage?from=checkout",
    });
  },

  /**
   * 显示历史地址列表
   */
  showAddressHistory() {
    if (this.data.addressHistory.length === 0) {
      wx.showToast({ title: "暂无历史地址", icon: "none" });
      return;
    }

    const items = this.data.addressHistory.map((item, index) => {
      return `${index + 1}. ${item.address}`;
    });

    wx.showActionSheet({
      itemList: items,
      success: (res) => {
        const selected = this.data.addressHistory[res.tapIndex];
        if (selected) {
          this.setData({
            deliveryAddress: selected.address,
            userLatitude: selected.latitude,
            userLongitude: selected.longitude,
            detailAddress: "", // 清除详细地址
          });
          wx.showToast({ title: "已选择历史地址", icon: "success" });
        }
      },
    });
  },

  /**
   * 清除地址
   */
  clearAddress() {
    this.setData({
      deliveryAddress: "",
      detailAddress: "",
      userLatitude: "",
      userLongitude: "",
    });
  },
});
