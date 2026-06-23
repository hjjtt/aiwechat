Page({
  data: {
    contactName: '',
    contactPhone: '',
    messageContent: '',
    faqList: [
      {
        id: 1,
        question: '如何修改订单信息？',
        answer: '订单创建后，如需要修改配送地址或联系方式，请尽快联系客服。如订单已进入配送状态，可能无法修改，敬请谅解。',
        expanded: false
      },
      {
        id: 2,
        question: '配送时间和范围是怎样的？',
        answer: '我们的配送时间为每天 10:00-21:00，配送范围为店铺周边 5 公里。具体配送费以结算页面显示为准。',
        expanded: false
      },
      {
        id: 3,
        question: '如何申请退款？',
        answer: '如遇到食品质量问题或送错餐品，请立即联系客服，我们会第一时间为您处理退款或重新配送。',
        expanded: false
      },
      {
        id: 4,
        question: '如何获取优惠券？',
        answer: '关注我们的微信公众号或加入会员群，定期会有优惠券发放。新用户注册也可获得新人优惠券礼包。',
        expanded: false
      },
      {
        id: 5,
        question: '餐品过敏原信息在哪里查看？',
        answer: '每道菜品的详情页都有详细的配料表和过敏原提示。如有特殊过敏史，建议下单前联系客服确认。',
        expanded: false
      }
    ]
  },

  onLoad() {
    // 页面加载
  },

  // 输入称呼
  onNameInput(e) {
    this.setData({
      contactName: e.detail.value
    })
  },

  // 输入电话
  onPhoneInput(e) {
    this.setData({
      contactPhone: e.detail.value
    })
  },

  // 输入留言内容
  onMessageInput(e) {
    this.setData({
      messageContent: e.detail.value
    })
  },

  // 拨打电话
  callPhone() {
    wx.showModal({
      title: '客服电话',
      content: '400-123-4567',
      confirmText: '拨打',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          wx.makePhoneCall({
            phoneNumber: '4001234567'
          })
        }
      }
    })
  },

  // 复制微信
  copyWechat() {
    wx.setClipboardData({
      data: 'renshi_kefu',
      success: () => {
        wx.showToast({
          title: '已复制微信号',
          icon: 'success'
        })
      }
    })
  },

  // 复制 QQ
  copyQQ() {
    wx.setClipboardData({
      data: '12345678',
      success: () => {
        wx.showToast({
          title: '已复制 QQ 号',
          icon: 'success'
        })
      }
    })
  },

  // 发送邮件
  sendEmail() {
    wx.showModal({
      title: '客服邮箱',
      content: 'kefu@renshi.com',
      confirmText: '复制邮箱',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          wx.setClipboardData({
            data: 'kefu@renshi.com',
            success: () => {
              wx.showToast({
                title: '已复制邮箱地址',
                icon: 'success'
              })
            }
          })
        }
      }
    })
  },

  // 展开/收起 FAQ
  toggleFaq(e) {
    const id = e.currentTarget.dataset.id
    const faqList = this.data.faqList.map(item => {
      if (item.id === id) {
        return { ...item, expanded: !item.expanded }
      }
      return item
    })
    this.setData({ faqList })
  },

  // 提交留言
  submitMessage() {
    if (!this.data.messageContent.trim()) {
      wx.showToast({
        title: '请填写留言内容',
        icon: 'none'
      })
      return
    }

    wx.showLoading({
      title: '提交中...',
      mask: true
    })

    // 这里可以调用后端 API 保存留言
    // 暂时模拟提交成功
    setTimeout(() => {
      wx.hideLoading()
      wx.showToast({
        title: '留言提交成功',
        icon: 'success'
      })

      // 清空表单
      this.setData({
        contactName: '',
        contactPhone: '',
        messageContent: ''
      })
    }, 1000)
  },

  // 跳转 AI 客服
  goToAIChat() {
    wx.navigateTo({
      url: '/pages/chat/chat'
    })
  },

  // 跳转意见反馈
  goToFeedback() {
    wx.navigateTo({
      url: '/pages/feedback/feedback'
    })
  },

  // 跳转订单
  goToOrders() {
    wx.switchTab({
      url: '/pages/orders/orders'
    })
  },

  // 跳转帮助中心
  goToHelp() {
    wx.showModal({
      title: '帮助中心',
      content: '1. 如需点餐，请进入"浏览菜单"选择菜品\n2. 如有疑问，可直接向 AI 客服提问\n3. 订单状态可在"我的订单"中查看',
      showCancel: false,
      confirmText: '知道了'
    })
  }
})
