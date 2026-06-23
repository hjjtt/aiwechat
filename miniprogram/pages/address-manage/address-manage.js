const { address: addressAPI } = require("../../utils/api.js");
const locationUtils = require("../../utils/location.js");

function getUserId() {
  const userId = wx.getStorageSync("userId");
  if (!userId) return null;
  if (typeof userId === "string" && userId.startsWith("user_")) {
    return parseInt(userId.replace("user_", ""), 10);
  }
  return parseInt(userId, 10);
}

Page({
  data: {
    contactName: "",
    contactPhone: "",
    deliveryAddress: "",
    detailAddress: "",
    userLatitude: "",
    userLongitude: "",
    addressLoading: false,
    isDefault: false,
    label: "家",
    labelIndex: 0,
    labels: ["家", "公司", "学校", "其他"],
    savedAddresses: [],
    editingAddress: null,
    pageMode: "add",
  },

  onLoad(options) {
    this.loadSavedAddresses();
    this.loadUserInfo();
    this.initLocation();

    if (options.id) {
      this.setData({ pageMode: "edit" });
      this.loadAddressDetail(parseInt(options.id));
    }
  },

  onShow() {
    const token = wx.getStorageSync("token");
    if (!token) {
      wx.redirectTo({ url: "/pages/login/login" });
    }
  },

  initLocation() {
    wx.showLoading({ title: "定位中...", mask: true });

    // 先检查定位权限状态
    wx.getSetting()
      .then((settingRes) => {
        const authSetting = settingRes.authSetting;
        if (authSetting["scope.userLocation"] === false) {
          // 用户之前拒绝过权限
          wx.hideLoading();
          wx.showModal({
            title: "需要定位权限",
            content: "请允许获取您的位置信息，以便自动填充配送地址",
            confirmText: "去设置",
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.openSetting();
              }
            },
          });
          return Promise.reject("location denied");
        }

        // 请求定位权限
        return wx.getLocation({
          type: "gcj02",
          highAccuracy: true,
        });
      })
      .then((res) => {
        this.reverseGeocode(res.latitude, res.longitude);
      })
      .catch((err) => {
        wx.hideLoading();
        console.error("定位失败:", err);
        // 不再自动提示，让用户手动点击选择位置
      });
  },

  reverseGeocode(latitude, longitude) {
    // 使用 location.js 工具类的逆地理编码（更可靠）
    locationUtils
      .reverseGeocode(latitude, longitude)
      .then((result) => {
        if (result && result.formattedAddress) {
          this.setData({
            deliveryAddress: result.formattedAddress,
            userLatitude: latitude,
            userLongitude: longitude,
          });
        }
        wx.hideLoading();
      })
      .catch((err) => {
        console.error("逆地理编码失败:", err);
        wx.hideLoading();
        wx.showToast({ title: "定位失败", icon: "none" });
      });
  },

  loadAddressDetail(id) {
    wx.showLoading({ title: "加载中..." });
    addressAPI
      .getAddress(id)
      .then((res) => {
        const addr = res.data || {};
        this.setData({
          editingAddress: addr,
          contactName: addr.contactName || "",
          contactPhone: addr.contactPhone || "",
          deliveryAddress: addr.address || "",
          detailAddress: addr.detailAddress || "",
          userLatitude: addr.latitude || "",
          userLongitude: addr.longitude || "",
          isDefault: addr.isDefault || false,
          label: addr.label || "家",
          labelIndex: ["家", "公司", "学校", "其他"].indexOf(
            addr.label || "家"
          ),
        });
        wx.hideLoading();
      })
      .catch((err) => {
        console.error("加载地址失败:", err);
        wx.hideLoading();
        wx.showToast({ title: "加载失败", icon: "none" });
      });
  },

  loadSavedAddresses() {
    const userId = getUserId();
    if (!userId) return;
    addressAPI
      .getAddresses(userId)
      .then((res) => {
        this.setData({ savedAddresses: res.data || [] });
      })
      .catch((err) => {
        console.error("加载地址列表失败:", err);
      });
  },

  loadUserInfo() {
    const nickname = wx.getStorageSync("nickname") || "";
    const phone = wx.getStorageSync("phone") || "";
    this.setData({
      contactName: nickname,
      contactPhone: phone,
    });
  },

  onNameInput(e) {
    this.setData({ contactName: e.detail.value });
  },

  onPhoneInput(e) {
    this.setData({ contactPhone: e.detail.value });
  },

  onDetailAddressInput(e) {
    this.setData({ detailAddress: e.detail.value });
  },

  selectLabel(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      labelIndex: index,
      label: this.data.labels[index],
    });
  },

  toggleDefault(e) {
    this.setData({ isDefault: e.detail.value });
  },

  chooseLocation() {
    wx.chooseLocation({
      latitude: this.data.userLatitude || undefined,
      longitude: this.data.userLongitude || undefined,
      success: (res) => {
        this.setData({
          deliveryAddress: res.address || res.name || "",
          userLatitude: res.latitude,
          userLongitude: res.longitude,
        });
      },
      fail: (err) => {
        if (err.errMsg && !err.errMsg.includes("cancel")) {
          wx.showToast({ title: "选择位置失败", icon: "none" });
        }
      },
    });
  },

  saveAddress() {
    if (!this.data.contactName || !this.data.contactName.trim()) {
      wx.showToast({ title: "请输入收货人", icon: "none" });
      return;
    }
    if (!this.data.contactPhone || !this.data.contactPhone.trim()) {
      wx.showToast({ title: "请输入手机号", icon: "none" });
      return;
    }
    const phoneRegex = /^1[3-9]\d{9}$/;
    if (!phoneRegex.test(this.data.contactPhone.trim())) {
      wx.showToast({ title: "请输入正确的手机号", icon: "none" });
      return;
    }
    if (!this.data.deliveryAddress || !this.data.deliveryAddress.trim()) {
      wx.showToast({ title: "请选择配送地址", icon: "none" });
      return;
    }

    const userId = getUserId();
    if (!userId) {
      wx.showToast({ title: "请先登录", icon: "none" });
      return;
    }

    const addressData = {
      userId: userId,
      contactName: this.data.contactName.trim(),
      contactPhone: this.data.contactPhone.trim(),
      address: this.data.deliveryAddress.trim(),
      detailAddress: (this.data.detailAddress || "").trim(),
      latitude: parseFloat(this.data.userLatitude) || null,
      longitude: parseFloat(this.data.userLongitude) || null,
      isDefault: this.data.isDefault,
      label: this.data.label,
    };

    wx.showLoading({ title: "保存中..." });
    const apiCall =
      this.data.pageMode === "edit" && this.data.editingAddress
        ? addressAPI.updateAddress(this.data.editingAddress.id, addressData)
        : addressAPI.addAddress(addressData);

    apiCall
      .then(() => {
        wx.hideLoading();
        wx.showToast({
          title: this.data.pageMode === "edit" ? "更新成功" : "添加成功",
          icon: "success",
        });
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      })
      .catch((err) => {
        wx.hideLoading();
        console.error("保存地址失败:", err);
        wx.showToast({ title: "保存失败", icon: "none" });
      });
  },

  editAddress(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/address-manage/address-manage?id=${id}` });
  },
});
