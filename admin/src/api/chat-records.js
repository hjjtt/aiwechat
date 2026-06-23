import request from './request'

// 获取聊天记录列表
export function getChatRecords(params) {
  return request.get('/admin/chat-records', { params })
}

// 删除单条聊天记录
export function deleteChatRecord(id) {
  return request.delete(`/admin/chat-records/${id}`)
}

// 批量删除聊天记录
export function batchDeleteChatRecords(ids) {
  return request.post('/admin/chat-records/batch-delete', { ids })
}

// 清空所有聊天记录
export function clearAllChatRecords() {
  return request.delete('/admin/chat-records/clear')
}

// 按时间清理聊天记录
export function cleanChatRecordsByDays(days) {
  return request.post('/admin/chat-records/clean-by-days', { days })
}

// 获取统计信息
export function getChatStats() {
  return request.get('/admin/chat-records/stats')
}

// 获取所有用户ID列表
export function getAllUserIds() {
  return request.get('/admin/chat-records/user-ids')
}
