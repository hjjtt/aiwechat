const { aiChat, order, wechat, address } = require("../../utils/api.js");
const chatHistory = require("../../utils/chatHistory.js");
const app = getApp();

Page({
  data: {
    messages: [],
    inputValue: "",
    canSend: false,
    loading: false,
    scrollToView: "scroll-bottom",
    inputFocused: true,
    userId: "",
    sessionId: "",
    isLoggedIn: false,
    userContext: {},
    pendingImages: [],
    keyboardHeight: 0,
    safeAreaBottom: 0,
    // 历史对话相关
    showHistoryModal: false,
    historyList: [],
    currentHistoryId: null,
  },

  onLoad(options) {
    const systemInfo = wx.getSystemInfoSync();
    const safeAreaInsets = systemInfo.safeAreaInsets;
    this.setData({
      safeAreaBottom: safeAreaInsets && safeAreaInsets.bottom ? safeAreaInsets.bottom : 0
    });
    this.checkLogin();
    this.loadHistoryList();
    this.setupKeyboardListener();
  },

  onShow() {
    this.checkLogin();
    this.loadHistoryList();
  },

  onUnload() {
    wx.offKeyboardHeightChange(this.onKeyboardHeightChange);
  },

  setupKeyboardListener() {
    wx.onKeyboardHeightChange((res) => {
      this.setData({
        keyboardHeight: res.height
      });
    });
  },

  onKeyboardHeightChange(res) {
    this.setData({
      keyboardHeight: res.height
    });
  },

  checkLogin() {
    const token = wx.getStorageSync("token");
    if (!token) {
      wx.redirectTo({
        url: "/pages/login/login",
      });
      return;
    }

    const userId = wx.getStorageSync("userId") || "";
    const nickname = wx.getStorageSync("nickname") || "用户";

    this.setData({
      isLoggedIn: !!token,
      userId: userId,
    });

    this.loadUserContext(userId, nickname);

    const pages = getCurrentPages();
    const currentPage = pages[pages.length - 1];
    if (currentPage.options.question) {
      this.setData({
        inputValue: decodeURIComponent(currentPage.options.question),
      });
      this.sendMessage();
    }
  },

  loadUserContext(userId, nickname) {
    let userIdNum = userId;
    if (typeof userId === "string" && userId.startsWith("user_")) {
      userIdNum = userId.replace("user_", "");
    }
    userIdNum = parseInt(userIdNum) || 0;
    if (!userIdNum) return;

    const context = {
      nickname: nickname,
      orderCount: 0,
      pendingOrderCount: 0,
      deliveringOrderCount: 0,
      completedOrderCount: 0,
      recentOrders: [],
      defaultAddress: "",
    };

    order
      .getUserOrders(userIdNum)
      .then((res) => {
        const orders = res.data || [];
        context.orderCount = orders.length;
        context.pendingOrderCount = orders.filter(
          (o) => o.status === "pending"
        ).length;
        context.deliveringOrderCount = orders.filter(
          (o) => o.status === "delivering"
        ).length;
        context.completedOrderCount = orders.filter(
          (o) => o.status === "completed"
        ).length;
        context.recentOrders = orders.slice(0, 3).map((o) => ({
          orderNumber: o.orderNumber,
          status: o.status,
          totalAmount: o.totalAmount,
          createdAt: o.createdAt,
        }));
        this.setData({ userContext: context });
      })
      .catch((err) => {
        console.error("加载订单失败:", err);
      });

    address
      .getDefaultAddress(userIdNum)
      .then((res) => {
        if (res.data) {
          context.defaultAddress = res.data.address || "";
          this.setData({ userContext: context });
        }
      })
      .catch((err) => {
        console.log("无默认地址:", err);
      });
  },

  onInput(e) {
    const value = e.detail.value;
    const hasContent =
      value.trim().length > 0 || this.data.pendingImages.length > 0;
    this.setData({
      inputValue: value,
      canSend: hasContent && !this.data.loading,
    });
  },

  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        const pendingImages = [...this.data.pendingImages, tempFilePath];
        const hasContent =
          this.data.inputValue.trim().length > 0 || pendingImages.length > 0;
        this.setData({
          pendingImages: pendingImages,
          canSend: hasContent && !this.data.loading,
        });
      },
    });
  },

  removeImage(e) {
    const index = e.currentTarget.dataset.index;
    const pendingImages = this.data.pendingImages.filter((_, i) => i !== index);
    const hasContent =
      this.data.inputValue.trim().length > 0 || pendingImages.length > 0;
    this.setData({
      pendingImages: pendingImages,
      canSend: hasContent && !this.data.loading,
    });
  },

  sendMessage() {
    const question = this.data.inputValue.trim();
    if (!question && this.data.pendingImages.length === 0) return;

    if (
      !this.data.userContext ||
      this.data.userContext.orderCount === undefined
    ) {
      wx.showLoading({ title: "加载中..." });
      const userId = this.data.userId;
      const nickname = wx.getStorageSync("nickname") || "用户";
      this.loadUserContext(userId, nickname);
      setTimeout(() => {
        wx.hideLoading();
        this.doSendMessage(question);
      }, 500);
      return;
    }

    this.doSendMessage(question);
  },

  doSendMessage(question) {
    const { pendingImages } = this.data;
    const userMsg = {
      id: Date.now(),
      role: "user",
      content: question,
      images: pendingImages,
    };
    const messages = [...this.data.messages, userMsg];

    this.setData({
      messages,
      inputValue: "",
      canSend: false,
      loading: true,
      scrollToView: "scroll-bottom",
      pendingImages: [],
    });

    const sendToAI = (imageUrls) => {
      aiChat
        .ask(question, this.data.userId, {
          ...this.data.userContext,
          images: imageUrls,
        })
        .then((res) => {
          const assistantMsg = {
            id: Date.now() + 1,
            role: "assistant",
            content: res.reply || "抱歉，我现在有点忙，请稍后再试。",
          };

          this.setData({
            messages: [...messages, assistantMsg],
            loading: false,
            scrollToView: "scroll-bottom",
          });

          // 保存对话到历史记录
          const updatedMessages = [...messages, assistantMsg];
          if (this.data.currentHistoryId) {
            // 更新现有历史记录
            chatHistory.updateChatHistoryMessages(this.data.userId, this.data.currentHistoryId, updatedMessages);
          } else {
            // 创建新的历史记录
            chatHistory.saveChatHistory(this.data.userId, updatedMessages);
            // 更新历史记录列表
            this.loadHistoryList();
          }
        })
        .catch((err) => {
          console.error("请求失败:", err);
          this.setData({ loading: false });

          wx.showToast({
            title: "请求失败，请重试",
            icon: "none",
          });
        });
    };

    if (pendingImages.length > 0) {
      const uploadPromises = pendingImages.map((filePath) => {
        return aiChat.uploadImage(filePath);
      });

      Promise.all(uploadPromises)
        .then((results) => {
          const imageUrls = results.map((res) => res.url);
          sendToAI(imageUrls);
        })
        .catch((err) => {
          console.error("图片上传失败:", err);
          wx.showToast({
            title: "图片上传失败",
            icon: "none",
          });
          this.setData({ loading: false });
        });
    } else {
      sendToAI([]);
    }
  },

  pageScrollToBottom() {
    this.setData({
      scrollToView: "scroll-bottom",
    });
  },

  // ========== 历史对话相关方法 ==========

  // 加载历史记录列表
  loadHistoryList() {
    const userId = this.data.userId;
    if (!userId) return;

    const historyList = chatHistory.getChatHistoryList(userId);
    this.setData({
      historyList: historyList,
    });
  },

  // 显示历史记录弹窗
  showHistory() {
    this.loadHistoryList();
    this.setData({
      showHistoryModal: true,
    });
  },

  // 关闭历史记录弹窗
  closeHistory() {
    this.setData({
      showHistoryModal: false,
    });
  },

  // 选择某个历史记录继续聊天
  selectHistory(e) {
    const historyId = e.currentTarget.dataset.id;
    const historyDetail = chatHistory.getChatHistoryDetail(this.data.userId, historyId);

    if (!historyDetail) {
      wx.showToast({
        title: '加载失败',
        icon: 'none',
      });
      return;
    }

    // 设置当前消息为历史记录的消息
    this.setData({
      messages: historyDetail.messages,
      currentHistoryId: historyId,
      showHistoryModal: false,
      scrollToView: 'scroll-bottom',
    });

    wx.showToast({
      title: '已加载历史对话',
      icon: 'success',
    });
  },

  // 删除某条历史记录
  deleteHistory(e) {
    const historyId = e.currentTarget.dataset.id;

    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条对话记录吗？',
      success: (res) => {
        if (res.confirm) {
          chatHistory.deleteChatHistory(this.data.userId, historyId);
          this.loadHistoryList();

          // 如果删除的是当前选中的记录，清空当前消息
          if (this.data.currentHistoryId === historyId) {
            this.setData({
              messages: [],
              currentHistoryId: null,
            });
          }

          wx.showToast({
            title: '已删除',
            icon: 'success',
          });
        }
      },
    });
  },

  // 清空所有历史记录
  clearAllHistory() {
    wx.showModal({
      title: '确认清空',
      content: '确定要清空所有对话记录吗？此操作不可恢复！',
      confirmColor: '#f08080',
      success: (res) => {
        if (res.confirm) {
          chatHistory.clearAllChatHistory(this.data.userId);
          this.loadHistoryList();
          this.setData({
            messages: [],
            currentHistoryId: null,
          });
          wx.showToast({
            title: '已清空',
            icon: 'success',
          });
        }
      },
    });
  },

  // 开始新对话（清除当前对话）
  startNewChat() {
    if (this.data.messages.length > 0) {
      // 保存当前对话到历史记录
      chatHistory.saveChatHistory(this.data.userId, this.data.messages);
    }

    this.setData({
      messages: [],
      currentHistoryId: null,
      showHistoryModal: false,
    });
  },
});
