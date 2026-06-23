const { order } = require("../../utils/api.js");

Page({
  data: {
    orders: [],
    currentStatus: "",
    userId: "",
    isLoggedIn: false,
    loading: true, // 添加加载状态
    error: "", // 添加错误信息
  },

  onShow() {
    this.loadOrders();
  },

  onLoad() {
    const token = wx.getStorageSync("token");
    if (!token) {
      wx.redirectTo({
        url: "/pages/login/login",
      });
      return;
    }

    const userId = wx.getStorageSync("userId") || "";

    this.setData({
      isLoggedIn: !!token,
      userId: userId,
    });
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadOrders().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 加载订单
  loadOrders() {
    return new Promise((resolve) => {
      const token = wx.getStorageSync("token");
      if (!token) {
        this.setData({ loading: false });
        wx.redirectTo({
          url: "/pages/login/login",
        });
        resolve();
        return;
      }

      const userId = wx.getStorageSync("userId") || "";

      if (!userId) {
        this.setData({ loading: false, error: "用户信息获取失败" });
        resolve();
        return;
      }

      this.setData({
        userId: userId,
        loading: true,
        error: "",
      });

      // 处理 userId，可能是 "user_10" 格式或纯数字
      let userIdStr = userId;
      if (typeof userIdStr === "string" && userIdStr.startsWith("user_")) {
        userIdStr = userIdStr.substring(5);
      }
      const userIdNum = parseInt(userIdStr) || 0;

      console.log(
        "正在加载订单, userId:",
        userIdNum,
        "status:",
        this.data.currentStatus
      );

      const request = this.data.currentStatus
        ? order.getUserOrdersByStatus(userIdNum, this.data.currentStatus)
        : order.getUserOrders(userIdNum);

      request
        .then((res) => {
          console.log("订单响应:", res);
          console.log("res.success:", res?.success);
          console.log("res.data:", res?.data);
          console.log("Array.isArray(res.data):", Array.isArray(res?.data));
          if (res && res.success && Array.isArray(res.data)) {
            const orders = res.data.map((item) => ({
              ...item,
              id: item.id || item.orderId,
              createdAt: this.formatTime(item.createdAt),
            }));
            this.setData({
              orders: orders,
              loading: false,
              error: "",
            });
          } else if (res && Array.isArray(res)) {
            const orders = res.map((item) => ({
              ...item,
              id: item.id || item.orderId,
              createdAt: this.formatTime(item.createdAt),
            }));
            this.setData({
              orders: orders,
              loading: false,
              error: "",
            });
          } else {
            this.setData({
              orders: [],
              loading: false,
              error: res?.message || "暂无订单数据",
            });
          }
          resolve(res);
        })
        .catch((err) => {
          console.error("加载订单失败:", err);
          this.setData({
            loading: false,
            error: "加载失败，请下拉刷新重试",
          });
          wx.showToast({
            title: "加载订单失败",
            icon: "none",
          });
          resolve(err);
        });
    });
  },

  // 格式化时间
  formatTime(timeStr) {
    if (!timeStr) return "";
    if (typeof timeStr === "string") return timeStr;
    if (timeStr instanceof Date) {
      return timeStr.toLocaleString("zh-CN");
    }
    return String(timeStr);
  },

  // 切换状态筛选
  switchStatus(e) {
    const status = e.currentTarget.dataset.status;
    this.setData({ currentStatus: status });
    this.loadOrders();
  },

  // 获取状态文本
  getStatusText(status) {
    const statusMap = {
      pending: "待处理",
      confirmed: "已确认",
      preparing: "准备中",
      delivering: "配送中",
      completed: "已完成",
      cancelled: "已取消",
    };
    return statusMap[status] || status || "未知";
  },

  // 取消订单
  cancelOrder(e) {
    const orderNumber = e.currentTarget.dataset.ordernumber;

    wx.showModal({
      title: "取消订单",
      content: "确定要取消这个订单吗？",
      success: (res) => {
        if (res.confirm) {
          // 处理 userId，可能是 "user_10" 格式或纯数字
          let userIdStr = this.data.userId;
          if (typeof userIdStr === "string" && userIdStr.startsWith("user_")) {
            userIdStr = userIdStr.substring(5);
          }
          const userIdNum = parseInt(userIdStr) || 0;
          console.log("取消订单, userId:", userIdNum);

          order
            .cancel(orderNumber, userIdNum)
            .then(() => {
              wx.showToast({ title: "订单已取消" });
              this.loadOrders();
            })
            .catch((err) => {
              console.error("取消订单失败:", err);
              wx.showToast({
                title: err.message || "取消失败",
                icon: "none",
              });
            });
        }
      },
    });
  },

  // 确认收货
  confirmReceipt(e) {
    const orderNumber = e.currentTarget.dataset.ordernumber;

    wx.showModal({
      title: "确认收货",
      content: "请确认已收到商品，确定要完成此订单吗？",
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: "处理中..." });

          order
            .updateStatus(orderNumber, "completed")
            .then(() => {
              wx.hideLoading();
              wx.showToast({ title: "确认收货成功", icon: "success" });
              this.loadOrders();
            })
            .catch((err) => {
              wx.hideLoading();
              console.error("确认收货失败:", err);
              wx.showToast({
                title: err.message || "操作失败",
                icon: "none",
              });
            });
        }
      },
    });
  },

  // 去点餐
  goToMenu() {
    wx.switchTab({
      url: "/pages/menu/menu",
    });
  },

  // 去支付
  goToPay(e) {
    const orderNumber = e.currentTarget.dataset.ordernumber;
    wx.navigateTo({
      url: "/pages/pay/pay?orderNumber=" + orderNumber,
    });
  },

  // 查看订单详情
  viewOrderDetail(e) {
    const orderNumber = e.currentTarget.dataset.ordernumber;
    // 可以跳转到订单详情页，或者显示弹窗
    wx.showActionSheet({
      itemList: ["查看详情", "再来一单"],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 查看详情
          wx.showModal({
            title: "订单详情",
            showCancel: false,
            content: `订单号: ${orderNumber}\n请联系客服获取更多信息`,
          });
        } else {
          // 再来一单
          wx.switchTab({
            url: "/pages/menu/menu",
          });
        }
      },
    });
  },
});
