import request from './request'

// 获取反馈列表
export function getFeedbackList(params) {
  return request.get('/admin/feedback', { params })
}

// 获取统计信息
export function getFeedbackStats() {
  return request.get('/admin/feedback/stats')
}

// 删除反馈
export function deleteFeedback(id) {
  return request.delete(`/admin/feedback/${id}`)
}

// 批量删除反馈
export function batchDeleteFeedback(ids) {
  return request.post('/admin/feedback/batch-delete', { ids })
}

// 清空所有反馈
export function clearAllFeedback() {
  return request.delete('/admin/feedback/clear')
}
