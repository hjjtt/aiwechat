# 目录说明

> 项目结构与启动流程详见 [README.md](README.md)；本页给出根目录各条目的用途，区分源码与运行时产物。

## 源码与配置（纳入版本控制）

```text
aiwechat/
├── backend/          Spring Cloud 微服务父工程（pom.xml 在此目录，不在根）
│   ├── common-core/      公共依赖：DTO、异常、JWT、拦截器（所有服务依赖）
│   ├── api-gateway/      网关（端口 9090，reactive，无 DevTools）
│   ├── auth-service/     认证（9091）
│   ├── menu-service/     菜单（9093）
│   ├── order-service/    订单（9097）
│   ├── ai-chat-service/  AI 对话（9094）
│   ├── knowledge-service/ RAG 知识库（9095）
│   └── admin-service/    管理后台 API（9096）
├── admin/            Vue3 + Vite + Element Plus 管理后台（npm run dev）
├── miniprogram/      微信小程序原生源码
├── scripts/
│   ├── sql/          数据库初始化脚本（init-database.sql、aiwechat.sql）
│   ├── nacos/        publish-config.ps1 + templates/*.tpl（渲染后发布到 Nacos）
│   └── cloud/        start-services.ps1 / run-service.ps1 一键启动
├── config/nacos/     本地 Nacos 安装与配置（data/ 已 gitignore，含渲染后密钥）
├── docs/             项目文档（API、RAG 配置、Nacos 启动等）
├── CLAUDE.md         Claude Code 项目指南（最详细）
├── AGENTS.md         AI chat service 关键事实
├── README.md         项目总览与快速开始
├── .env.example      环境变量模板（复制为 .env 填写）
├── .gitignore
└── .reloadtrigger    Spring DevTools 触发文件（见 HOTRELOAD.md）
```

## 运行时产物（已 gitignore，勿提交）

- `.env` — 本地敏感配置（密钥、DB 密码等）
- `logs/`、`*.log`（含 `backend.log`）— 运行日志
- `target/` — Maven 构建产物
- `uploaded/`、`uploads/`、`data/` — 运行时上传与数据文件
- `node_modules/`（admin 下）、`.idea/`、`.vscode/`、`.m2/` — 依赖与 IDE 配置
- `nul` — Windows 保留设备名误生成文件（已 gitignore）

## 说明

- 微服务父 pom 在 `backend/pom.xml`，不在根目录。
- 每个服务独立运行：先 `mvn install -pl common-core -am -DskipTests`，再 `mvn spring-boot:run -pl <module>`（不要加 `-am`，否则对父 pom 执行 run 目标报 no main class，详见 CLAUDE.md）。
- Nacos 渲染后的真实密钥落盘在 `config/nacos/data/`，已 gitignore，绝不提交。
