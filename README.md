# AIWeChat · 微信小程序 AI 客服点餐系统

基于 Spring Cloud + 微信小程序 + Vue3 的智能客服点餐解决方案，集成 ModelScope 大模型实现 AI 对话、RAG 知识库、订单管理与人工转接。

---

## 项目简介

AIWeChat 面向餐饮/轻食门店，提供：

- **用户端**：微信小程序点餐、AI 智能客服问答、订单追踪、地址管理、收藏夹。
- **管理端**：Vue3 + Element Plus 后台，管理用户、订单、菜单、聊天记录。
- **服务端**：Spring Cloud 微服务架构，含网关、认证、AI 对话、知识库、订单、菜单、管理后台等模块。
- **AI 能力**：基于 ModelScope Qwen3 / GLM / Kimi 多模型轮询，支持 RAG 知识库 PDF 检索与人工转接。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2、Spring Cloud 2023、Spring Cloud Alibaba 2023、Spring AI 1.1、MyBatis-Plus 3.5、JDK 17 |
| 网关 | Spring Cloud Gateway |
| 注册/配置 | Nacos |
| 数据库 | MySQL 8 |
| 缓存/限流 | Redis（可选） |
| 向量存储 | 本地 JSON 向量存储 / Spring AI Vector Store |
| 管理端 | Vue 3、Vite、Element Plus、Pinia、Axios |
| 用户端 | 微信小程序原生框架 |
| AI 模型 | ModelScope（Qwen3-1.7B 等）、智谱 Embedding |

---

## 项目结构

```text
aiwechat/
├── backend/                    # Spring Cloud 后端微服务
│   ├── common-core/            # 公共依赖与工具
│   ├── api-gateway/            # API 网关（端口 9000）
│   ├── auth-service/           # 认证服务（端口 9091）
│   ├── menu-service/           # 菜单服务（端口 9092）
│   ├── order-service/          # 订单服务
│   ├── ai-chat-service/        # AI 对话服务
│   ├── knowledge-service/      # RAG 知识库服务
│   └── admin-service/          # 管理后台服务
├── admin/                      # Vue3 管理后台
├── miniprogram/                # 微信小程序源码
├── scripts/
│   ├── sql/                    # 数据库初始化脚本
│   ├── nacos/                  # Nacos 配置发布脚本
│   └── cloud/                  # 微服务一键启动脚本
├── config/                     # 本地基础设施配置（Nacos 等）
├── docs/                       # 项目文档
├── .env.example                # 环境变量示例
└── README.md                   # 本文件
```

---

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.9+
- MySQL 8
- Node.js 18+（管理端）
- 微信开发者工具（小程序）
- 可选：Redis、Nacos

### 2. 克隆与配置

```bash
git clone https://github.com/hjjtt/aiwechat.git
cd aiwechat

# 复制环境变量模板并填写真实密钥
cp .env.example .env
```

`.env` 中需要配置的关键项：

```properties
DB_URL=jdbc:mysql://localhost:3306/aiwechat?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password

WECHAT_APP_ID=your_wechat_app_id
WECHAT_APP_SECRET=your_wechat_app_secret

MODELSCOPE_API_KEY=your_modelscope_api_key
EMBEDDING_API_KEY=your_embedding_api_key
AMAP_API_KEY=your_amap_api_key
```

> 完整配置说明见 `.env.example`。

### 3. 初始化数据库

```bash
mysql -u root -p < scripts/sql/init-database.sql
mysql -u root -p aiwechat < scripts/sql/aiwechat.sql
```

### 4. 启动后端

#### 方式 A：本地单体/简化启动（推荐开发调试）

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run -pl admin-service -am      # 示例：启动管理后台服务
```

#### 方式 B：本地 Nacos + 微服务启动

```bash
# 1. 启动 Nacos（config/nacos/ 下有相关配置）
# 2. 发布 Nacos 配置
powershell -File scripts/nacos/publish-config.ps1

# 3. 一键启动所有服务
powershell -File scripts/cloud/start-services.ps1
```

详细流程见 `docs/cloud-本地Nacos三服务启动流程.md`。

### 5. 启动管理端

```bash
cd admin
npm install
npm run dev
```

默认访问 http://localhost:5173，账号密码见 `.env.example` 中的 `ADMIN_AUTH_USERNAME` / `ADMIN_AUTH_PASSWORD`。

### 6. 启动小程序

使用微信开发者工具打开 `miniprogram` 目录，填入你的小程序 AppID，编译预览。

---

## 核心功能

| 功能 | 说明 |
|------|------|
| 微信登录 | `wx.login` 获取 code，后端换取 openId 并签发 Token |
| AI 智能客服 | 多模型轮询对话，支持上下文与 RAG 知识库检索 |
| 知识库管理 | 上传 PDF，自动切分、Embedding 并支持检索问答 |
| 人工转接 | 复杂问题可转人工客服处理 |
| 菜单点餐 | 分类浏览、搜索、加购、收藏 |
| 订单管理 | 下单、取消、状态流转、后台改单 |
| 地址管理 | 高德地图选址、地址增删改查 |
| 后台管理 | 用户、订单、菜单、聊天记录可视化运营 |

---

## 订单状态流转

```
pending（待确认） → confirmed（已确认） → preparing（制作中） → delivering（配送中） → completed（已完成）
                                     ↘ cancelled（已取消）
```

---

## 主要服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| API Gateway | 9000 | 统一入口 |
| Auth Service | 9091 | 微信登录、Token 认证 |
| Menu Service | 9092 | 菜单管理 |
| Order Service | - | 订单管理 |
| AI Chat Service | - | AI 对话 |
| Knowledge Service | - | RAG 知识库 |
| Admin Service | - | 管理后台 API |

---

## 文档

- [API 文档](docs/API.md)
- [RAG 配置指南](docs/RAG_SETUP_GUIDE.md)
- [本地 Nacos 三服务启动流程](docs/cloud-本地Nacos三服务启动流程.md)
- [技术文档](docs/技术文档.md)
- [Claude 项目指南](CLAUDE.md)

---

## 注意事项

- 生产环境请务必修改默认管理员密码、Nacos Token、AI 密钥等敏感配置。
- `.env` 文件已加入 `.gitignore`，请勿将真实密钥提交到仓库。
- Windows 下 `nul` 为保留设备名，项目已将其加入 `.gitignore`，无需理会。

---

## License

MIT
