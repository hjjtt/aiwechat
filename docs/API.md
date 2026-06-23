# 微信点餐小程序 API 文档

**Base URL**: `http://localhost:8081`

---

## 通用说明

### 响应格式
所有 API 返回统一格式：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {...}
}
```

### 认证
需要认证的接口需在 Header 中携带：
```
Authorization: Bearer {token}
```

---

## 微信登录 API

### 1. 模拟登录（测试用）
**POST** `/api/wechat/mock-login`

**请求体**:
```json
{
  "nickname": "张三",
  "avatarUrl": "头像URL"
}
```

**响应**:
```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "token": "jwt-token",
    "userId": 123,
    "nickname": "张三",
    "avatarUrl": "/uploads/avatars/xxx.jpg",
    "isNewUser": false
  }
}
```

---

### 2. 微信登录
**POST** `/api/wechat/login`

**请求体**:
```json
{
  "code": "微信code",
  "userInfo": {
    "nickName": "微信昵称",
    "avatarUrl": "微信头像URL"
  }
}
```

**响应**: 同模拟登录

---

### 3. 获取用户信息
**GET** `/api/wechat/user/info`

**Header**: `Authorization: Bearer {token}`

**响应**:
```json
{
  "success": true,
  "data": {
    "id": 123,
    "openId": "wx_openid",
    "nickname": "张三",
    "avatarUrl": "/uploads/avatars/xxx.jpg",
    "status": "active"
  }
}
```

---

### 4. 退出登录
**POST** `/api/wechat/logout`

**Header**: `Authorization: Bearer {token}`

**响应**:
```json
{
  "success": true,
  "message": "退出成功"
}
```

---

## 订单管理 API

### 1. 创建订单
**POST** `/api/orders`

**Header**: `Authorization: Bearer {token}`

**请求体**:
```json
{
  "userId": 123,
  "totalAmount": 58.00,
  "contactName": "张三",
  "contactPhone": "13800138000",
  "deliveryAddress": "北京市朝阳区xxx",
  "remark": "备注",
  "items": [
    {
      "menuId": 1,
      "menuName": "宫保鸡丁",
      "quantity": 2,
      "unitPrice": 28.00,
      "subtotal": 56.00
    }
  ]
}
```

**响应**:
```json
{
  "success": true,
  "message": "订单创建成功",
  "data": {
    "orderId": 1,
    "orderNumber": "ORD202602142A7451",
    "status": "pending",
    "totalAmount": 58.00,
    "createdAt": "2026-02-14T20:00:00"
  }
}
```

---

### 2. 获取订单详情（ID）
**GET** `/api/orders/id/{id}`

**响应**:
```json
{
  "success": true,
  "data": {订单详情对象}
}
```

---

### 3. 获取订单详情（订单号）
**GET** `/api/orders/{orderNumber}`

**响应**: 同上

---

### 4. 获取用户订单列表
**GET** `/api/orders/user/{userId}`

**响应**:
```json
{
  "success": true,
  "data": [
    {订单1},
    {订单2}
  ]
}
```

---

### 5. 获取用户某状态订单
**GET** `/api/orders/user/{userId}/status/{status}`

**参数**: `status` = `pending` | `confirmed` | `preparing` | `delivering` | `completed` | `cancelled`

---

### 6. 更新订单状态
**POST** `/api/orders/{orderNumber}/status`

**参数**: `status` = 新状态

**响应**:
```json
{
  "success": true,
  "message": "状态更新成功"
}
```

---

### 7. 取消订单
**POST** `/api/orders/{orderNumber}/cancel`

**参数**: `userId` = 用户ID

**响应**:
```json
{
  "success": true,
  "message": "订单已取消"
}
```

---

## 菜品管理 API

### 1. 获取所有上架菜品
**GET** `/api/menu/items`

**响应**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "宫保鸡丁",
      "description": "经典川菜",
      "price": 28.00,
      "priceFormatted": "¥28.00",
      "category": "川菜",
      "isAvailable": true,
      "salesCount": 120
    }
  ]
}
```

---

### 2. 根据分类获取菜品
**GET** `/api/menu/category/{category}`

**参数**: `category` = 菜品分类

---

### 3. 搜索菜品
**GET** `/api/menu/search`

**参数**: `keyword` = 搜索关键词

---

### 4. 获取菜品详情
**GET** `/api/menu/items/{itemId}`

---

## 地址管理 API

### 1. 获取用户所有地址
**GET** `/api/addresses/user/{userId}`

**响应**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 123,
      "contactName": "张三",
      "contactPhone": "13800138000",
      "address": "北京市朝阳区xxx",
      "isDefault": true
    }
  ]
}
```

---

### 2. 获取默认地址
**GET** `/api/addresses/user/{userId}/default`

**响应**: 同单个地址对象

---

### 3. 获取地址详情
**GET** `/api/addresses/{id}`

---

### 4. 添加地址
**POST** `/api/addresses`

**Header**: `Authorization: Bearer {token}`

**请求体**:
```json
{
  "userId": 123,
  "contactName": "张三",
  "contactPhone": "13800138000",
  "address": "北京市朝阳区xxx"
}
```

**响应**:
```json
{
  "success": true,
  "message": "添加成功",
  "data": {创建的地址对象}
}
```

---

### 5. 更新地址
**PUT** `/api/addresses/{id}`

**请求体**: 同添加地址

**响应**:
```json
{
  "success": true,
  "message": "更新成功"
}
```

---

### 6. 删除地址
**DELETE** `/api/addresses/{id}`

**响应**:
```json
{
  "success": true,
  "message": "删除成功"
}
```

---

### 7. 设置默认地址
**POST** `/api/addresses/{id}/default`

**请求体**:
```json
{
  "userId": 123
}
```

**响应**:
```json
{
  "success": true,
  "message": "已设为默认地址"
}
```

---

## 收藏管理 API

### 1. 获取用户收藏列表
**GET** `/api/favorites/user/{userId}`

**响应**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 123,
      "menuId": 1,
      "menuName": "宫保鸡丁",
      "menuPrice": 28.00,
      "createdAt": "2026-02-14T20:00:00"
    }
  ]
}
```

---

### 2. 检查是否已收藏
**GET** `/api/favorites/check`

**参数**: 
- `userId` - 用户ID
- `menuId` - 菜品ID

**响应**:
```json
{
  "success": true,
  "data": {
    "isFavorite": true
  }
}
```

---

### 3. 添加收藏
**POST** `/api/favorites`

**请求体**:
```json
{
  "userId": 123,
  "menuId": 1
}
```

---

### 4. 取消收藏
**DELETE** `/api/favorites`

**请求体**: 同添加收藏

---

### 5. 切换收藏状态
**POST** `/api/favorites/toggle`

**请求体**:
```json
{
  "userId": 123,
  "menuId": 1
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "isFavorite": false,
    "message": "取消收藏成功"
  }
}
```

---

### 6. 获取收藏数量
**GET** `/api/favorites/count/{userId}`

**响应**:
```json
{
  "success": true,
  "data": 5
}
```

---

## 文件上传 API

### 1. 上传头像
**POST** `/api/upload/avatar`

**参数**:
- `file` - MultipartFile (图片，最大 2MB)

**响应**:
```json
{
  "success": true,
  "message": "上传成功",
  "data": {
    "url": "/uploads/avatars/20260214/xxx.jpg",
    "filename": "xxx.jpg"
  }
}
```

---

### 2. 上传一般文件
**POST** `/api/upload/file`

**参数**: 
- `file` - MultipartFile

**响应**:
```json
{
  "success": true,
  "message": "上传成功",
  "data": {
    "url": "/uploads/files/20260214/xxx.jpg",
    "filename": "xxx.jpg",
    "originalName": "原始文件名.jpg"
  }
}
```

---

## AI 聊天 API

### 1. 提问
**POST** `/api/ai-chat/ask`

**请求体**:
```json
{
  "question": "用户问题",
  "userId": "123",
  "sessionId": "会话ID（可选）",
  "nickname": "张三",
  "orderCount": 10,
  "pendingOrderCount": 1,
  "deliveringOrderCount": 2,
  "completedOrderCount": 7,
  "defaultAddress": "北京市朝阳区xxx",
  "images": ["/uploads/chat-images/xxx.jpg"]
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "reply": "AI回答内容",
    "sources": ["通用知识"],
    "sessionId": "session-uuid"
  }
}
```

**说明**:
- 有图片时自动使用 `moonshotai/Kimi-K2.5` 模型
- 无图片时使用模型轮换（Qwen3-1.7B → GLM-4.7-Flash → Kimi-K2.5 → MiniMax-M2.5）

---

### 2. 获取对话历史
**GET** `/api/ai-chat/history/{userId}`

**响应**:
```json
{
  "success": true,
  "data": {
    "userId": "123",
    "history": "用户: 你好\n客服: 你好，有什么可以帮您？\n用户: 我想点餐..."
  }
}
```

---

### 3. 上传聊天图片
**POST** `/api/ai-chat/upload`

**Header**: `Authorization: Bearer {token}`

**参数**:
- `file` - MultipartFile (图片)

**响应**:
```json
{
  "url": "/uploads/chat-images/xxx.jpg"
}
```

---

### 4. 健康检查
**GET** `/api/admin/health`

**响应**:
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "AI Chat Service"
  }
}
```

---

## 知识库管理 API

### 1. 上传文档
**POST** `/api/knowledge/upload`

**参数**:
- `file` - MultipartFile
- `metadata` - 文档元数据（可选，JSON字符串）

**响应**:
```json
{
  "success": true,
  "message": "上传成功",
  "data": {
    "success": true,
    "message": "文档上传成功",
    "fileName": "文件.pdf",
    "documentIds": ["doc-1", "doc-2"]
  }
}
```

---

### 2. 批量上传文档
**POST** `/api/knowledge/upload/batch`

**参数**: `files` - MultipartFile[]

---

### 3. 添加文本
**POST** `/api/knowledge/add-text`

**参数**:
- `content` - 文本内容
- `title` - 标题

**响应**:
```json
{
  "success": true,
  "data": {
    "success": true,
    "documentId": "doc-123"
  }
}
```

---

### 4. 批量添加文本
**POST** `/api/knowledge/add-texts`

**请求体**:
```json
[
  {"content": "文本1", "title": "标题1"},
  {"content": "文本2", "title": "标题2"}
]
```

---

### 5. 搜索知识库
**GET** `/api/knowledge/search`

**参数**:
- `query` - 搜索关键词
- `topK` = 5（返回结果数量）

**响应**:
```json
{
  "success": true,
  "data": {
    "query": "搜索词",
    "results": [
      {
        "documentId": "doc-1",
        "content": "文档内容片段",
        "score": 0.95
      }
    ]
  }
}
```

---

### 6. 列出所有文档
**GET** `/api/knowledge/list`

---

### 7. 删除文档
**DELETE** `/api/knowledge/{documentId}`

---

### 8. 清空知识库
**DELETE** `/api/knowledge/clear`

---

### 9. 初始化菜品知识库
**POST** `/api/knowledge/init-menu`

**说明**: 自动从菜单导入菜品信息到知识库

---

### 10. 手动同步知识库
**POST** `/api/knowledge/sync`

**响应**:
```json
{
  "success": true,
  "message": "知识库同步完成",
  "data": {
    "success": true,
    "message": "知识库同步完成",
    "menuCount": 50,
    "lastSyncTime": "2026-02-14T20:00:00"
  }
}
```

---

### 11. 获取同步状态
**GET** `/api/knowledge/sync/status`

**响应**:
```json
{
  "success": true,
  "data": {
    "isSyncing": false,
    "lastSyncTime": "2026-02-14T20:00:00",
    "menuCount": 50,
    "nextSyncTime": "2026-02-15T03:00:00"
  }
}
```

---

## 后台管理 API

### 后台菜单管理

#### 1. 分页查询菜品
**GET** `/api/admin/menus`

**参数**:
- `category` - 分类筛选（可选）
- `keyword` - 关键词搜索（可选）
- `page` = 1
- `size` = 20

**响应**:
```json
{
  "success": true,
  "data": {
    "items": [菜品列表],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```

---

#### 2. 获取菜品统计
**GET** `/api/admin/menus/stats`

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 100,
    "available": 80,
    "unavailable": 20,
    "categories": {"川菜": 30, "粤菜": 25}
  }
}
```

---

#### 3. 获取所有分类
**GET** `/api/admin/menus/categories`

**响应**:
```json
{
  "success": true,
  "data": ["川菜", "粤菜", "湘菜", "饮品"]
}
```

---

#### 4. 添加菜品
**POST** `/api/admin/menus`

**请求体**:
```json
{
  "name": "宫保鸡丁",
  "description": "经典川菜",
  "price": 28.00,
  "category": "川菜",
  "isAvailable": true
}
```

---

#### 5. 更新菜品
**PUT** `/api/admin/menus/{id}`

---

#### 6. 删除菜品
**DELETE** `/api/admin/menus/{id}`

---

#### 7. 更新上架状态
**POST** `/api/admin/menus/{id}/availability`

**参数**: `available` = true/false

---

#### 8. 批量更新上架状态
**POST** `/api/admin/menus/batch-availability`

**请求体**:
```json
{
  "ids": [1, 2, 3],
  "available": true
}
```

---

### 后台订单管理

#### 1. 分页查询订单
**GET** `/api/admin/orders`

**参数**:
- `status` - 状态筛选（可选）
- `keyword` - 关键词搜索（可选）
- `page` = 1
- `size` = 20

---

#### 2. 获取订单统计
**GET** `/api/admin/orders/stats`

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 1000,
    "pending": 50,
    "confirmed": 30,
    "preparing": 20,
    "delivering": 10,
    "completed": 850,
    "cancelled": 40,
    "totalAmount": 50000.00
  }
}
```

---

#### 3. 获取订单详情
**GET** `/api/admin/orders/{id}`

---

#### 4. 更新订单状态
**POST** `/api/admin/orders/{id}/status`

**参数**: `status` = 新状态

---

#### 5. 批量更新订单状态
**POST** `/api/admin/orders/batch-status`

**请求体**:
```json
{
  "ids": [1, 2, 3],
  "status": "confirmed"
}
```

---

#### 6. 获取所有订单状态
**GET** `/api/admin/orders/statuses`

**响应**:
```json
{
  "success": true,
  "data": ["pending", "confirmed", "preparing", "delivering", "completed", "cancelled"]
}
```

---

### 后台用户管理

#### 1. 分页查询用户
**GET** `/api/admin/users`

**参数**:
- `keyword` - 关键词搜索（可选）
- `status` - 状态筛选（可选，active/banned）
- `page` = 1
- `size` = 20

---

#### 2. 获取用户统计
**GET** `/api/admin/users/stats`

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 1000,
    "active": 950,
    "banned": 50,
    "newUsersToday": 10
  }
}
```

---

#### 3. 获取用户详情
**GET** `/api/admin/users/{id}`

---

#### 4. 更新用户状态
**PUT** `/api/admin/users/{id}/status`

**请求体**:
```json
{
  "status": "banned"
}
```

---

#### 5. 删除用户
**DELETE** `/api/admin/users/{id}`

---

## 监控与管理 API

### 1. 获取性能指标
**GET** `/api/admin/metrics`

**响应**:
```json
{
  "success": true,
  "data": {
    "totalRequests": 10000,
    "successfulRequests": 9500,
    "failedRequests": 500,
    "averageResponseTime": 120,
    "activeSessions": 50
  }
}
```

---

### 2. 获取健康状态
**GET** `/api/admin/health`

**响应**:
```json
{
  "success": true,
  "data": {
    "status": "healthy",
    "database": "up",
    "aiService": "up",
    "diskUsage": "45%"
  }
}
```

---

### 3. 获取转接队列
**GET** `/api/admin/transfer/queue`

**响应**:
```json
{
  "success": true,
  "data": {
    "pendingCount": 5,
    "transfers": [
      {
        "transferId": "transfer-1",
        "userId": 123,
        "question": "用户问题",
        "createdAt": "2026-02-14T20:00:00",
        "status": "pending"
      }
    ]
  }
}
```

---

### 4. 客服接单
**POST** `/api/admin/transfer/{transferId}/accept`

**参数**: `agentId` = 客服ID

**响应**:
```json
{
  "success": true,
  "message": "接单成功",
  "data": {
    "success": true
  }
}
```

---

### 5. 获取所有反馈
**GET** `/api/admin/feedback`

---

### 6. 获取反馈统计
**GET** `/api/admin/feedback/stats`

**响应**:
```json
{
  "success": true,
  "data": {
    "averageRating": 4.5,
    "maxRating": 5,
    "minRating": 1,
    "totalFeedback": 100
  }
}
```

---

## 数据模型

### ApiResponse 通用响应
```typescript
{
  success: boolean,
  message: string,
  data: T | null
}
```

### LoginResponse 登录响应
```typescript
{
  token: string,
  userId: number,
  nickname: string,
  avatarUrl: string,
  isNewUser: boolean
}
```

### Order 订单对象
```typescript
{
  id: number,
  orderNumber: string,
  userId: number,
  status: 'pending' | 'confirmed' | 'preparing' | 'delivering' | 'completed' | 'cancelled',
  totalAmount: number,
  contactName: string,
  contactPhone: string,
  deliveryAddress: string,
  remark: string,
  createdAt: string,
  updatedAt: string
}
```

### MenuItem 菜品对象
```typescript
{
  id: number,
  name: string,
  description: string,
  price: number,
  priceFormatted: string,
  category: string,
  isAvailable: boolean,
  salesCount: number,
  imageUrl?: string
}
```

### UserAddress 地址对象
```typescript
{
  id: number,
  userId: number,
  contactName: string,
  contactPhone: string,
  address: string,
  isDefault: boolean
}
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未认证或 token 过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 配置说明

### ModelScope AI 配置
```yaml
spring:
  ai:
    openai:
      api-key: ${MODELSCOPE_API_KEY}
      base-url: https://api-inference.modelscope.cn/v1
      models:
        - qwen/Qwen3-1.7B
        - ZhipuAI/GLM-4.7-Flash
        - moonshotai/Kimi-K2.5  # 支持图片识别
        - MiniMax/MiniMax-M2.5
```

### 文件上传配置
```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 2MB
      max-request-size: 10MB
      location: ./uploads
```

---

## 注意事项

1. **Token 有效期**: 72 小时
2. **知识库同步**: 每天凌晨 3 点自动同步
3. **图片识别**: 有图片时自动使用 Kimi-2.5
4. **模型轮换**: 无图片时按顺序尝试不同模型，避免单日调用限制
5. **上传文件大小限制**: 头像 2MB，其他文件 10MB
