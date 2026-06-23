# Nacos 接入说明

## 目标

1. 使用本地 Nacos 托管配置。
2. 敏感配置不保存在项目 `application.yml` 中。
3. 通过 `.env` 和 Nacos OpenAPI 发布配置。
4. 所有 Nacos 相关文件统一存放在 `config/nacos`。

## 已落地内容

1. `config/nacos/publish-config.ps1`
   读取 `.env` 并通过 Nacos API 发布配置。
2. `config/nacos/templates/*.yaml.tpl`
   生成各服务的配置内容。
3. `auth-service`、`api-gateway`、`business-service`
   已改为从 Nacos 读取配置。

## 启动步骤

### 1. 准备 `.env`

基于 `.env.example` 填写本地敏感配置。

### 2. 启动本地 Nacos

请直接使用本机 Nacos 安装包或解压版。

Windows 常见启动方式：

```powershell
config\\nacos\\bin\\startup.cmd -m standalone
```

控制台地址通常为：

- `http://localhost:8848/nacos`
- 默认账号密码：`nacos / nacos`

### 3. 发布配置到 Nacos

```powershell
powershell -ExecutionPolicy Bypass -File config/nacos/publish-config.ps1
```

可选参数：

```powershell
powershell -ExecutionPolicy Bypass -File config/nacos/publish-config.ps1 `
  -EnvFile .env `
  -NacosServer http://127.0.0.1:8848 `
  -Group AIWECHAT_GROUP `
  -Namespace "" `
  -Username nacos `
  -Password nacos
```

### 4. 启动服务

1. `auth-service`
2. `business-service`
3. `api-gateway`

## Data ID 规划

1. `shared-config.yaml`
2. `auth-service.yaml`
3. `api-gateway.yaml`
4. `business-service.yaml`

## 说明

- 项目统一使用本地安装或独立进程方式维护基础设施。
- Nacos 文件统一放在 `config/nacos`。
