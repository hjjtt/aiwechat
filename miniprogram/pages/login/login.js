// pages/login/login.js
const app = getApp();

Page({
  data: {
    loading: false,
    errorMsg: "",
    nickname: "",
    avatarUrl: "",
    defaultAvatar:
      "data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%20100%20100%22%3E%3Ccircle%20cx%3D%2250%22%20cy%3D%2250%22%20r%3D%2250%22%20fill%3D%22%23ffeaa7%22%2F%3E%3Ctext%20x%3D%2250%22%20y%3D%2260%22%20text-anchor%3D%22middle%22%20font-size%3D%2240%22%3E%F0%9F%98%90%3C%2Ftext%3E%3C%2Fsvg%3E",
  },

  onShow() {
    // 已登录则跳转到首页
    const token = wx.getStorageSync("token");
    if (token) {
      wx.switchTab({
        url: "/pages/index/index",
      });
    }
  },

  // 昵称输入
  onNicknameInput(e) {
    this.setData({
      nickname: e.detail.value,
    });
  },

  // 昵称失焦时处理（兼容昵称获取）
  onNicknameBlur(e) {
    const nickname = e.detail.value || this.data.nickname;
    // 如果用户使用了微信昵称（type="nickname"），会尝试自动填充
    if (nickname && !this.data.nickname) {
      this.setData({ nickname });
    }
  },

  onChooseAvatar(e) {
    const that = this;

    if (e.detail && e.detail.avatarUrl) {
      const avatarUrl = e.detail.avatarUrl;
      console.log("微信选择头像:", avatarUrl);
      that.setData({ avatarUrl: avatarUrl });
      return;
    }

    wx.showActionSheet({
      itemList: ["从相册选择", "拍照"],
      success(res) {
        const sourceType = res.tapIndex === 0 ? ["album"] : ["camera"];

        wx.chooseMedia({
          count: 1,
          mediaType: ["image"],
          sourceType: sourceType,
          success(chooseRes) {
            const tempFilePath = chooseRes.tempFiles[0].tempFilePath;
            console.log("选择图片成功:", tempFilePath);

            that.setData({
              avatarUrl: tempFilePath,
            });

            that.uploadAvatar(tempFilePath);
          },
        });
      },
    });
  },

  // 上传头像到服务器
  uploadAvatar(filePath) {
    const that = this;
    wx.showLoading({ title: "上传中..." });

    wx.uploadFile({
      url: app.globalData.baseUrl + "/api/upload/avatar",
      filePath: filePath,
      name: "file",
      header: {
        Authorization: "Bearer " + (wx.getStorageSync("token") || ""),
      },
      success: (res) => {
        wx.hideLoading();
        console.log("上传响应:", res);

        if (res.statusCode === 200) {
          try {
            const data = JSON.parse(res.data);
            if (data.success && data.data) {
              that.setData({
                avatarUrl: data.data.url,
              });
              wx.showToast({
                title: "头像上传成功",
                icon: "success",
              });
            }
          } catch (e) {
            console.error("解析上传响应失败:", e);
          }
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error("上传失败:", err);
        wx.showToast({
          title: "上传失败，使用本地图片",
          icon: "none",
        });
      },
    });
  },

  // 微信登录
  handleWechatLogin() {
    if (this.data.loading) {
      console.log("已经在登录中，忽略重复点击");
      return;
    }

    // 验证昵称
    if (!this.data.nickname || this.data.nickname.trim() === "") {
      wx.showToast({
        title: "请输入昵称",
        icon: "none",
      });
      return;
    }

    this.setData({
      loading: true,
      errorMsg: "",
    });

    console.log("=== 开始模拟登录流程（本地开发） ===");
    // 本地开发直接走模拟登录，跳过微信授权，100%成功
    this.doMockLogin();
  },

  // 真正的微信登录
  doRealLogin(code) {
    const that = this;
    console.log("调用真正微信登录接口, code:", code);

    wx.request({
      url: app.globalData.baseUrl + "/api/auth/login",
      method: "POST",
      header: {
        "Content-Type": "application/json",
      },
      data: {
        code: code,
        userInfo: {
          nickName: that.data.nickname.trim(),
          avatarUrl: that.data.avatarUrl || "",
        },
      },
      success: (res) => {
        console.log("后端响应:", res.statusCode, res.data);

        if (
          res.statusCode === 200 &&
          res.data &&
          res.data.success &&
          res.data.data
        ) {
          console.log("微信登录成功!");
          that.handleLoginSuccess(res.data.data);
        } else {
          const errorMsg =
            res.data && res.data.error ? res.data.error : "登录失败";
          console.error("登录失败:", errorMsg);
          console.log("降级到模拟登录...");
          that.doMockLogin();
        }
      },
      fail: (err) => {
        console.error("请求失败:", err);
        console.log("请求失败，降级到模拟登录...");
        that.doMockLogin();
      },
    });
  },

  // 模拟登录（当真正微信登录失败时的降级方案）
  doMockLogin() {
    const that = this;
    console.log("调用模拟登录接口");

    wx.request({
      url: app.globalData.baseUrl + "/api/auth/mock-login",
      method: "POST",
      header: {
        "Content-Type": "application/json",
      },
      data: {
        nickname: this.data.nickname.trim(),
        avatarUrl: this.data.avatarUrl || "",
      },
      success: (res) => {
        console.log("后端响应:", res.statusCode, res.data);

        if (
          res.statusCode === 200 &&
          res.data &&
          res.data.success &&
          res.data.data
        ) {
          console.log("模拟登录成功!");
          that.handleLoginSuccess(res.data.data);
        } else {
          const errorMsg =
            res.data && res.data.error ? res.data.error : "登录失败";
          console.error("登录失败:", errorMsg);
          that.setData({
            loading: false,
            errorMsg: errorMsg,
          });
        }
      },
      fail: (err) => {
        console.error("请求失败:", err);
        that.setData({
          loading: false,
          errorMsg: "网络请求失败: " + (err.errMsg || "请检查后端服务"),
        });
      },
    });
  },

  // 打开协议
  openAgreement(e) {
    const type = e.currentTarget.dataset.type;
    wx.navigateTo({
      url: `/pages/agreement/agreement?type=${type}`
    });
  },

  // 处理登录成功
  handleLoginSuccess(loginData) {
    // 保存登录信息
    wx.setStorageSync("token", loginData.token);
    wx.setStorageSync("userId", loginData.userId);
    wx.setStorageSync("nickname", loginData.nickname);
    wx.setStorageSync("avatarUrl", loginData.avatarUrl);
    wx.setStorageSync("isNewUser", loginData.isNewUser);

    wx.showToast({
      title: loginData.isNewUser ? "欢迎新用户" : "欢迎回来",
      icon: "success",
    });

    // 更新全局用户信息
    if (app.globalData) {
      app.globalData.userInfo = {
        token: loginData.token,
        userId: loginData.userId,
        nickname: loginData.nickname,
        avatarUrl: loginData.avatarUrl,
      };
    }

    // 延迟跳转
    setTimeout(() => {
      wx.switchTab({
        url: "/pages/index/index",
      });
    }, 1500);
  },
});
