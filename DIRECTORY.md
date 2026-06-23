# 目录说明

## 根目录

```text
aiwechat/
├── src/                 Spring Boot 业务源码
├── miniprogram/         微信小程序
├── admin/               Vue 管理后台
├── auth-service/        认证服务
├── api-gateway/         网关服务
├── business-service/    迁移期业务服务
├── common-core/         公共模块
├── config/              本地配置与 Nacos 相关文件
├── docs/                项目文档
├── uploaded/            上传文件
├── logs/                日志
├── pom.xml              Maven 父工程
├── .env                 本地敏感配置
└── .env.example         环境变量示例
```

## 说明

- 项目当前采用本地服务或独立进程方式运行基础设施组件。
- Nacos 相关内容统一放在 `config/nacos/`。
- `target/`、`.idea/` 等目录属于构建产物或本地环境文件。
