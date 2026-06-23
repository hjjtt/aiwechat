import request from './request'

// 后台管理员登录（复用微信登录接口，实际项目建议单独实现）
export function login(username, password) {
  return request.post('/admin/login', {
    username,
    password
  })
}

// 退出登录
export function logout() {
  return request.post('/admin/logout')
}

// 获取用户信息
export function getUserInfo() {
  return request.get('/admin/profile')
}
