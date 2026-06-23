import request from './request'

// 获取用户列表
export function getUsers(params) {
  return request.get('/admin/users', { params })
}

// 获取用户详情
export function getUserById(id) {
  return request.get(`/admin/users/${id}`)
}

// 获取用户统计信息
export function getUserStats() {
  return request.get('/admin/users/stats')
}

// 更新用户状态
export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

// 删除用户
export function deleteUser(id) {
  return request.delete(`/admin/users/${id}`)
}
