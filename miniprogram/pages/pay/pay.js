const { order } = require("../../utils/api.js");

Page({
  data: {
    orderNumber: "",
    orderInfo: {
      orderNumber: "",
      totalAmount: 0,
      itemCount: 0,
      createdAt: "",
    },
    selectedPayMethod: "wechat",
    balance: "0.00",
    paying: false,
    userId: "",
  },

  onLoad(options) {
    const orderNumber = options.orderNumber || "";
    if (!orderNumber) {
      wx.showToast({ title: "订单不存在", icon: "none" });
      setTimeout(() => wx.navigateBack(), 1500);
      return;
    }

    this.setData({ orderNumber });
    this.loadOrderInfo(orderNumber);
    this.loadUserBalance();
  },

  loadOrderInfo(orderNumber) {
    wx.showLoading({ title: "加载中..." });

    order
      .getByNumber(orderNumber)
      .then((res) => {
        wx.hideLoading();
        if (res && res.data) {
          const orderData = res.data;
          const itemCount = orderData.items ? orderData.items.length : 0;
          this.setData({
            "orderInfo.orderNumber": orderData.orderNumber || orderNumber,
            "orderInfo.totalAmount": orderData.totalAmount || 0,
            "orderInfo.itemCount": itemCount,
            "orderInfo.createdAt": this.formatTime(orderData.createdAt),
          });
        }
      })
      .catch((err) => {
        wx.hideLoading();
        console.error("加载订单失败:", err);
        wx.showToast({ title: "加载订单失败", icon: "none" });
      });
  },

  loadUserBalance() {
    // 模拟余额，实际应该从后端获取
    this.setData({ balance: "0.00" });
  },

  formatTime(timeStr) {
    if (!timeStr) return "";
    if (typeof timeStr === "string") return timeStr;
    if (timeStr instanceof Date) {
      return timeStr.toLocaleString("zh-CN");
    }
    return String(timeStr);
  },

  goBack() {
    wx.navigateBack();
  },

  selectPayMethod(e) {
    const method = e.currentTarget.dataset.method;
    this.setData({ selectedPayMethod: method });
  },

  handlePay() {
    if (this.data.paying) return;

    const { orderNumber, selectedPayMethod, orderInfo } = this.data;

    if (!orderNumber || !orderInfo.totalAmount) {
      wx.showToast({ title: "订单信息错误", icon: "none" });
      return;
    }

    // 余额支付
    if (selectedPayMethod === "balance") {
      this.processBalancePay();
      return;
    }

    // 微信支付（模拟）
    this.processWechatPay();
  },

  processWechatPay() {
    const { orderNumber, orderInfo } = this.data;

    this.setData({ paying: true });

    // 模拟微信支付流程
    wx.showModal({
      title: "微信支付",
      content: `确认支付 ¥${orderInfo.totalAmount}？`,
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: "支付中..." });
          setTimeout(() => {
            wx.hideLoading();
            this.updateOrderStatus("confirmed");
          }, 1500);
        } else {
          this.setData({ paying: false });
        }
      },
      fail: () => {
        this.setData({ paying: false });
      },
    });
  },

  processBalancePay() {
    const { orderNumber, orderInfo, balance } = this.data;
    const balanceNum = parseFloat(balance);

    if (balanceNum < orderInfo.totalAmount) {
      wx.showToast({ title: "余额不足", icon: "none" });
      return;
    }

    this.setData({ paying: true });

    wx.showLoading({ title: "支付中..." });

    // 模拟余额支付
    setTimeout(() => {
      wx.hideLoading();
      this.updateOrderStatus("confirmed");
    }, 1500);
  },

  updateOrderStatus(status) {
    const { orderNumber } = this.data;

    // 处理 userId
    let userId = wx.getStorageSync("userId") || "";
    if (typeof userId === "string" && userId.startsWith("user_")) {
      userId = userId.replace("user_", "");
    }
    const userIdNum = parseInt(userId) || 0;

    order
      .updateStatus(orderNumber, status)
      .then(() => {
        wx.showToast({ title: "支付成功", icon: "success" });
        setTimeout(() => {
          wx.reLaunch({
            url: "/pages/index/index",
          });
        }, 1500);
      })
      .finally(() => {
        this.setData({ paying: false });
      });
  },
});
