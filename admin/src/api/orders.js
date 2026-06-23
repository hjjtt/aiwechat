import request from './request'

// 获取订单列表
export function getOrders(params) {
  return request.get('/admin/orders', { params })
}

// 获取订单详情
export function getOrderById(id) {
  return request.get(`/admin/orders/${id}`)
}

// 获取订单统计信息
export function getOrderStats() {
  return request.get('/admin/orders/stats')
}

// 更新订单状态
export function updateOrderStatus(id, status) {
  return request.post(`/admin/orders/${id}/status`, null, { params: { status } })
}

// 批量更新订单状态
export function batchUpdateOrderStatus(ids, status) {
  return request.post('/admin/orders/batch-status', { ids, status })
}

// 获取所有订单状态
export function getOrderStatuses() {
  return request.get('/admin/orders/statuses')
}
