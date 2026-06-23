const { feedback } = require('../../utils/api.js')

// 获取数字类型的 userId
function getUserId() {
  const userId = wx.getStorageSync('userId')
  if (!userId) return null
  if (typeof userId === 'string' && userId.startsWith('user_')) {
    return parseInt(userId.replace('user_', ''), 10)
  }
  return parseInt(userId, 10)
}

// 获取当前会话 ID（从聊天页面传递或新建）
function getSessionId() {
  const sessionId = wx.getStorageSync('sessionId')
  if (sessionId) return sessionId
  // 生成新的会话 ID
  const newSessionId = 'session_' + Date.now()
  wx.setStorageSync('sessionId', newSessionId)
  return newSessionId
}

Page({
  data: {
    rating: 0,
    ratingText: '请点击评分',
    question: '',
    aiAnswer: '',
    comment: '',
    feedbackHistory: [],
    sessionId: ''
  },

  onLoad(options) {
    // 从聊天页面传递的数据
    if (options.question) {
      this.setData({
        question: decodeURIComponent(options.question)
      })
    }
    if (options.answer) {
      this.setData({
        aiAnswer: decodeURIComponent(options.answer)
      })
    }
    
    const sessionId = getSessionId()
    this.setData({ sessionId })
    
    // 加载用户反馈历史
    this.loadFeedbackHistory()
  },

  // 选择评分
  selectRating(e) {
    const rating = e.currentTarget.dataset.rating
    const ratingTexts = [
      '',
      '非常不满意',
      '不满意',
      '一般',
      '满意',
      '非常满意'
    ]
    
    this.setData({
      rating,
      ratingText: ratingTexts[rating]
    })
  },

  // 问题描述输入
  onQuestionInput(e) {
    this.setData({
      question: e.detail.value
    })
  },

  // 详细意见输入
  onCommentInput(e) {
    this.setData({
      comment: e.detail.value
    })
  },

  // 加载反馈历史
  loadFeedbackHistory() {
    const userId = getUserId()
    if (!userId) return

    feedback.getUserFeedback(userId)
      .then(res => {
        const feedbackList = res.data || []
        // 格式化日期显示
        const formattedList = feedbackList.map(item => ({
          ...item,
          createdAt: this.formatDate(item.createdAt)
        }))
        this.setData({
          feedbackHistory: formattedList
        })
      })
      .catch(err => {
        console.error('加载反馈历史失败:', err)
      })
  },

  // 提交反馈
  submitFeedback() {
    const userId = getUserId()
    
    if (!userId) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      wx.navigateTo({
        url: '/pages/login/login'
      })
      return
    }

    if (this.data.rating === 0) {
      wx.showToast({
        title: '请先评分',
        icon: 'none'
      })
      return
    }

    wx.showLoading({
      title: '提交中...',
      mask: true
    })

    feedback.submitFeedback({
      userId: String(userId),
      sessionId: this.data.sessionId,
      question: this.data.question,
      aiAnswer: this.data.aiAnswer,
      rating: this.data.rating,
      comment: this.data.comment
    })
      .then(res => {
        wx.hideLoading()
        wx.showToast({
          title: '提交成功',
          icon: 'success'
        })
        
        // 重置表单
        this.setData({
          rating: 0,
          ratingText: '请点击评分',
          question: '',
          aiAnswer: '',
          comment: ''
        })
        
        // 刷新反馈历史
        this.loadFeedbackHistory()
      })
      .catch(err => {
        wx.hideLoading()
        console.error('提交反馈失败:', err)
        wx.showToast({
          title: '提交失败，请重试',
          icon: 'none'
        })
      })
  },

  // 格式化日期
  formatDate(dateStr) {
    if (!dateStr) return ''
    try {
      const date = new Date(dateStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    } catch (e) {
      return dateStr
    }
  }
})
