# Spring Boot DevTools 热加载

## 适用范围

`spring-boot-devtools` 已作为依赖加入 6 个服务：`auth-service`、`menu-service`、`order-service`、`ai-chat-service`、`knowledge-service`、`admin-service`。`api-gateway` 为 reactive 网关，未引入 DevTools。

> 当前为微服务架构，每个服务独立运行，不再有单体 `AiwechatApplication` 或 `aiwechat-*.jar`。

## 运行方式

```bash
# 1. 一次性：把 common-core 安装到本地仓库
mvn install -pl common-core -am -DskipTests

# 2. 以 DevTools 模式运行某个服务（不要加 -am，否则对父 pom 执行 run 目标会报 no main class）
mvn spring-boot:run -pl ai-chat-service
```

修改该服务 `src/main/java` 下的源码并重新编译后，DevTools 会在约 1 秒后自动重启该服务进程。

## IDE 配置

- 安装 Lombok 插件，并启用注解处理器：`Settings -> Build -> Compiler -> Annotation Processors`。
- IDEA 按 `Ctrl+F9`（Mac `Cmd+F9`）重新编译即触发重启。
- 修改 `application.yml` / 增删 Maven 依赖不会触发热重启，需手动重启。

## 触发文件（可选）

仓库根目录有 `.reloadtrigger` 文件。DevTools 的触发文件模式需在服务配置中设置 `spring.devtools.restart.trigger-file` 指向它才生效；当前 `application.yml` 与 Nacos 模板未配置此项，故默认走 classpath 变更自动重启。如需启用：

```yaml
spring:
  devtools:
    restart:
      trigger-file: .reloadtrigger
```

## 注意事项

- 生产环境 DevTools 自动禁用（`java -jar` 启动时不生效）。
- LiveReload 默认启用，需浏览器插件配合刷新页面。
- 热重启只作用于当前运行的单个服务；跨服务改动需分别重启对应服务。
