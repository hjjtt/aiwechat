# 本地 Nacos + 配置发布 + 三服务启动流程

本文档用于本项目本地联调（不使用 Docker）。

## 1. 前置条件

- JDK 17
- Maven 3.9+
- MySQL 已启动，`aiwechat` 库可访问
- Nacos 已解压到 `config/nacos`，并可本地启动
- 根目录存在 `.env`（可由 `.env.example` 复制）

## 2. `.env` 必填项

下面这些变量为空会导致配置发布失败：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `WECHAT_APP_ID`
- `WECHAT_APP_SECRET`
- `MODELSCOPE_API_KEY`
- `AMAP_API_KEY`

建议额外确认：

- `NACOS_GROUP`（默认 `AIWECHAT_GROUP`）
- `NACOS_NAMESPACE`（建议使用专用 namespace ID，避免和其他项目混用）

## 3. 启动本地 Nacos

在项目根目录执行：

```powershell
config\nacos\bin\startup.cmd -m standalone
```

控制台地址：

- [http://127.0.0.1:8848/nacos](http://127.0.0.1:8848/nacos)

## 4. 校验并发布配置

先做校验（不发布）：

```powershell
powershell -ExecutionPolicy Bypass -File config/nacos/publish-config.ps1 -EnvFile .env -CheckOnly
```

校验通过后正式发布：

```powershell
powershell -ExecutionPolicy Bypass -File config/nacos/publish-config.ps1 -EnvFile .env
```

会发布 4 个 DataId：

- `shared-config.yaml`
- `auth-service.yaml`
- `api-gateway.yaml`
- `business-service.yaml`

## 5. 启动三个服务

### 方式 A：一键启动（推荐）

```powershell
powershell -ExecutionPolicy Bypass -File scripts/cloud/start-services.ps1 -EnvFile .env
```

该脚本会自动做三件事：

1. 检查 Nacos 是否可访问
2. 执行配置发布
3. 分别拉起 `auth-service`、`business-service`、`api-gateway`（新终端窗口）

如需跳过发布（只重启服务）：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/cloud/start-services.ps1 -EnvFile .env -SkipPublish
```

### 方式 B：手工启动

分别在 3 个终端执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/cloud/run-service.ps1 -Module auth-service -EnvFile .env
powershell -ExecutionPolicy Bypass -File scripts/cloud/run-service.ps1 -Module business-service -EnvFile .env
powershell -ExecutionPolicy Bypass -File scripts/cloud/run-service.ps1 -Module api-gateway -EnvFile .env
```

## 6. 联调检查

- 网关健康检查：[http://127.0.0.1:8080/actuator/health](http://127.0.0.1:8080/actuator/health)
- 认证服务转发检查：`POST /api/auth/mock-login`（走网关）
- 业务接口检查：`GET /api/menu/items`（走网关）

## 7. 本轮已补齐的缺口

- 发布脚本支持 `-CheckOnly` 和 `-DryRun`，可先校验后发布
- 发布脚本增加必填项校验，避免半配置启动
- 发布脚本改为基于脚本目录解析模板路径，减少相对路径误用
- 发布脚本支持 `NACOS_AUTH_IDENTITY_KEY` / `NACOS_AUTH_IDENTITY_VALUE` / `NACOS_AUTH_TOKEN`
- 增加三服务启动脚本，避免每次手工配置环境变量

## 8. 仍建议你尽快做的两件事

1. 在 Nacos 控制台创建项目专用 namespace，并把 `.env` 的 `NACOS_NAMESPACE` 改成该 namespace ID。
2. 轮换 `.env` 中已暴露过的真实密钥（尤其是第三方 API key）。
