import request from './request'

// 获取菜单列表
export function getMenus(params) {
  return request.get('/admin/menus', { params })
}

// 获取菜单详情
export function getMenuById(id) {
  return request.get(`/admin/menus/${id}`)
}

// 获取菜单统计信息
export function getMenuStats() {
  return request.get('/admin/menus/stats')
}

// 获取所有分类
export function getMenuCategories() {
  return request.get('/admin/menus/categories')
}

// 添加菜单
export function addMenu(data) {
  return request.post('/admin/menus', data)
}

// 更新菜单
export function updateMenu(id, data) {
  return request.put(`/admin/menus/${id}`, data)
}

// 删除菜单
export function deleteMenu(id) {
  return request.delete(`/admin/menus/${id}`)
}

// 更新上架状态
export function updateMenuAvailability(id, available) {
  return request.post(`/admin/menus/${id}/availability`, null, { params: { available } })
}

// 批量更新上架状态
export function batchUpdateMenuAvailability(ids, available) {
  return request.post('/admin/menus/batch-availability', { ids, available })
}
