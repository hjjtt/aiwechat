const app = getApp();
const { order } = require("../../utils/api.js");

Page({
  data: {
    userInfo: {},
    userId: "",
    nickname: "",
    avatarUrl: "",
    isLoggedIn: false,
    orderStats: {
      pending: 0,
      paid: 0,
      delivering: 0,
      completed: 0,
    },
  },

  onShow() {
    this.loadUserData();
  },

  onLoad() {
    this.loadUserData();
  },

  // 加载用户数据
  loadUserData() {
    const token = wx.getStorageSync("token");
    const nickname = wx.getStorageSync("nickname") || "游客用户";
    const avatarUrl =
      wx.getStorageSync("avatarUrl") || "/images/default-avatar.png";
    const userId = wx.getStorageSync("userId") || "";

    this.setData({
      isLoggedIn: !!token,
      nickname: nickname,
      avatarUrl: avatarUrl,
      userId: userId,
      userInfo: {
        nickName: nickname,
        avatarUrl: avatarUrl,
      },
    });

    // 加载订单统计数据
    if (token && userId) {
      this.loadOrderStats(userId);
    }
  },

  // 加载订单统计数据
  loadOrderStats(userId) {
    // 处理 userId 格式
    let userIdNum = userId;
    if (typeof userId === "string" && userId.startsWith("user_")) {
      userIdNum = userId.replace("user_", "");
    }
    userIdNum = parseInt(userIdNum) || 0;
    if (!userIdNum) return;

    order
      .getUserOrders(userIdNum)
      .then((res) => {
        const orders = res.data || [];
        const stats = {
          pending: orders.filter((o) => o.status === "pending").length,
          paid: orders.filter(
            (o) => o.status === "confirmed" || o.status === "preparing"
          ).length,
          delivering: orders.filter((o) => o.status === "delivering").length,
          completed: orders.filter((o) => o.status === "completed").length,
        };
        this.setData({ orderStats: stats });
      })
      .catch((err) => {
        console.error("加载订单统计失败:", err);
      });
  },

  // 跳转登录页
  goToLogin() {
    wx.navigateTo({
      url: "/pages/login/login",
    });
  },

  // 跳转客服
  navigateToChat() {
    wx.navigateTo({
      url: "/pages/chat/chat",
    });
  },

  // 跳转订单
  navigateToOrders() {
    wx.switchTab({
      url: "/pages/orders/orders",
    });
  },

  // 跳转收货地址管理
  showAddress() {
    if (!this.data.isLoggedIn) {
      this.goToLogin();
      return;
    }
    wx.navigateTo({
      url: "/pages/address-manage/address-manage",
    });
  },

  // 显示收藏
  showFavorites() {
    if (!this.data.isLoggedIn) {
      this.goToLogin();
      return;
    }
    wx.navigateTo({
      url: "/pages/favorites/favorites",
    });
  },

  // 显示帮助
  showHelp() {
    wx.showModal({
      title: "帮助中心",
      content:
        '1. 如需点餐，请进入"浏览菜单"选择菜品\n2. 如有疑问，可直接向AI客服提问\n3. 订单状态可在"我的订单"中查看',
      showCancel: false,
      confirmText: "知道了",
    });
  },

  // 联系客服
  contactService() {
    wx.navigateTo({
      url: "/pages/contact/contact",
    });
  },

  // 意见反馈
  showFeedback() {
    if (!this.data.isLoggedIn) {
      this.goToLogin();
      return;
    }
    wx.navigateTo({
      url: "/pages/feedback/feedback",
    });
  },

  // 关于我们
  showAbout() {
    wx.showModal({
      title: "关于我们",
      content: "任氏外卖 AI点餐助手 v1.0\n\n基于AI技术的智能点餐系统",
      showCancel: false,
      confirmText: "知道了",
    });
  },

  // 退出登录
  logout() {
    wx.showModal({
      title: "退出登录",
      content: "确定要退出登录吗？",
      success: (res) => {
        if (res.confirm) {
          app.logout();
        }
      },
    });
  },
});
