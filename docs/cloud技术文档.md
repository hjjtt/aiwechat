# AIWechat Spring Cloud 微服务改造技术文档

## 1. 文档目标

本文档用于指导 `aiwechat` 项目从当前单体 Spring Boot 架构，逐步演进为可维护、可扩展的 Spring Cloud 微服务架构。

本文档重点不是纯理论介绍，而是面向当前仓库的实际落地，回答以下问题：

1. 当前项目为什么适合分阶段改造。
2. 应该拆成哪些服务。
3. 应该先做哪些准备工作。
4. 每个阶段要改哪些代码、配置、数据库和前端调用。
5. 改造过程中有哪些风险，如何规避。

## 2. 当前项目现状

当前项目是典型的单体后端架构：

1. 后端只有一个 Spring Boot 应用入口。
2. 所有 Controller、Service、Repository 运行在同一进程。
3. 所有业务共享一个 MySQL 数据库。
4. 小程序和管理后台都直接请求同一个后端服务。
5. 登录态使用自定义 token，并保存在 `user_sessions` 表。
6. AI 问答、知识库、订单、菜单、地址、收藏、后台统计都在同一个服务内。

### 2.1 当前架构的优点

1. 开发简单，部署简单。
2. 本地事务容易控制。
3. 适合项目早期快速迭代。

### 2.2 当前架构的局限

1. AI 能力、订单能力、管理后台无法独立扩容。
2. 模块之间是代码级强耦合，边界不清晰。
3. 一处异常可能影响整个系统。
4. 后续多人并行开发时容易互相冲突。
5. 配置、鉴权、限流、日志、监控都集中在一个应用中，不利于扩展。

## 3. 是否适合改为 Spring Cloud

结论：适合，但不建议一步到位重构成完整分布式系统，建议采用“模块化单体 -> 核心服务拆分 -> 完整微服务化”的渐进式改造。

### 3.1 适合改造的原因

当前项目已经具备较明显的业务边界：

1. 用户与登录
2. 菜单与商品
3. 订单
4. 地址与收藏
5. AI 对话
6. 知识库
7. 管理后台

其中 AI 对话和知识库天然适合独立部署，因为：

1. 它们依赖外部大模型接口。
2. 请求耗时长，负载模式和订单完全不同。
3. 后续可能单独扩展异步处理、队列和向量数据库。

### 3.2 不建议一步到位的原因

如果直接从单体切到完整 Spring Cloud，会同时引入以下复杂度：

1. 服务注册发现
2. 网关转发
3. 分布式鉴权
4. 服务间调用
5. 分布式事务或最终一致性
6. 配置中心
7. 链路追踪与监控
8. 数据库拆分

当前项目体量还没有大到必须一次性吃下全部复杂度，因此最优解是分阶段实施。

## 4. 推荐目标架构

推荐采用 Spring Cloud Alibaba 或标准 Spring Cloud 体系，核心组件如下：

1. `api-gateway`
   作用：统一入口、路由转发、跨域、限流、鉴权。

2. `auth-service`
   作用：微信登录、token 管理、用户会话校验、用户基础信息。

3. `user-service`
   作用：用户资料、地址、收藏、用户偏好。

4. `product-service`
   作用：菜单、分类、商品检索、上下架、销量统计。

5. `order-service`
   作用：订单创建、订单状态流转、订单明细。

6. `ai-service`
   作用：AI 聊天、聊天记录、模型路由、人工转接、异步处理。

7. `knowledge-service`
   作用：知识文档上传、向量化、RAG 检索、菜单知识同步。

8. `admin-bff`
   作用：为后台管理端提供聚合接口。
   说明：规模不大时也可先由网关直连各服务，不强制单独拆。

9. `common-core`
   作用：公共 DTO、统一返回结构、异常码、鉴权模型、日志规范。

10. `common-client`
   作用：各服务之间的 Feign 接口或 HTTP Client 封装。

### 4.1 推荐部署关系

```text
微信小程序 / Vue Admin
        |
        v
   API Gateway
   /   |    \
  v    v     v
Auth  Order  AI
 |      |     |
 v      v     v
User  Product Knowledge
```

## 5. 服务边界设计

### 5.1 auth-service

负责：

1. 微信登录
2. mock 登录
3. token 生成
4. token 校验
5. logout
6. session 生命周期管理

迁移来源：

1. `WechatController`
2. `WechatLoginService`
3. `TokenServiceImpl`
4. `UserSessionRepository`

不负责：

1. 订单
2. 菜单
3. AI 对话

### 5.2 user-service

负责：

1. 用户基本信息
2. 用户地址
3. 收藏
4. 用户偏好

迁移来源：

1. `AddressController`
2. `FavoriteController`
3. `UserRepository`
4. `UserAddressRepository`
5. `UserFavoriteRepository`
6. `UserPreferenceRepository`

### 5.3 product-service

负责：

1. 菜单列表
2. 分类查询
3. 搜索
4. 商品详情
5. 菜单管理后台
6. 上下架

迁移来源：

1. `MenuController`
2. `AdminMenuController`
3. `MenuServiceImpl`
4. `AdminMenuServiceImpl`
5. `MenuItemRepository`

### 5.4 order-service

负责：

1. 创建订单
2. 订单状态流转
3. 订单详情
4. 用户订单列表
5. 后台订单管理

迁移来源：

1. `OrderController`
2. `AdminOrderController`
3. `OrderServiceImpl`
4. `AdminOrderService`
5. `OrderRepository`
6. `OrderItemRepository`

注意：

订单创建时依赖商品价格校验，后续必须改成通过 `product-service` 获取商品快照或价格快照，不能继续直接查本地商品表。

### 5.5 ai-service

负责：

1. AI 问答
2. 聊天记录
3. 图片问答
4. 模型轮换
5. 人工转接
6. 异步 AI 处理
7. 性能统计

迁移来源：

1. `AIChatController`
2. `ModelScopeAICustomerServiceImpl`
3. `AsyncAiServiceImpl`
4. `HumanTransferServiceImpl`
5. `ChatRecordRepository`

### 5.6 knowledge-service

负责：

1. 文档上传
2. 文本切分
3. 向量化
4. RAG 检索
5. 菜单知识初始化
6. 文档同步任务

迁移来源：

1. `KnowledgeController`
2. `KnowledgeBaseServiceImpl`
3. `KnowledgeSyncServiceImpl`

注意：

当前知识库初始化直接依赖菜单表。拆分后应改为调用 `product-service` 的开放接口获取菜单数据。

## 6. 技术选型建议

## 6.1 基础框架

推荐版本：

1. Spring Boot `3.2.x`
2. Spring Cloud `2023.x`
3. Spring Cloud Alibaba 对应兼容版本
4. Java `17`

### 6.2 推荐组件

1. 注册中心：Nacos
2. 配置中心：Nacos Config
3. 网关：Spring Cloud Gateway
4. 服务调用：OpenFeign
5. 熔断限流：Sentinel 或 Resilience4j
6. 分布式链路追踪：Micrometer Tracing + Zipkin/Tempo
7. 日志采集：ELK 或 Loki
8. 消息队列：RabbitMQ 或 RocketMQ
9. 缓存：Redis
10. 向量库：
   当前阶段可先保留 Spring AI + 本地向量存储。
   中后期推荐迁移到 Milvus、PGVector 或 Elasticsearch 向量检索。

## 7. 数据库拆分建议

不建议一开始就完全物理拆库，建议分两步：

### 7.1 第一阶段：逻辑拆分，物理仍共库

做法：

1. 每个服务只访问自己负责的表。
2. 用包边界和数据库访问边界约束依赖。
3. 禁止跨服务直接引用别人的 Repository。

推荐表归属：

1. `auth-service`
   - `user_sessions`

2. `user-service`
   - `users`
   - `user_addresses`
   - `user_favorites`
   - `user_preferences`

3. `product-service`
   - `menu_items`

4. `order-service`
   - `orders`
   - `order_items`

5. `ai-service`
   - `chat_records`

6. `knowledge-service`
   - `knowledge_documents`

### 7.2 第二阶段：物理拆库

等服务稳定后，再拆成独立数据库：

1. `auth_db`
2. `user_db`
3. `product_db`
4. `order_db`
5. `ai_db`
6. `knowledge_db`

注意事项：

1. 拆库后不能继续本地 join。
2. 统计类接口必须改成聚合查询或离线汇总。
3. 订单创建必须用商品快照，而不是跨库实时 join。

## 8. 鉴权改造方案

当前项目里，很多接口仍然由前端传 `userId`。这在微服务场景下风险较高。

目标改造方式：

1. 小程序登录后获取 token。
2. 所有请求只带 `Authorization: Bearer <token>`。
3. Gateway 或 auth-service 负责校验 token。
4. 校验通过后，将 `userId`、`openId`、`roles` 等用户信息写入请求头，转发给下游服务。
5. 下游服务不再相信前端直接传入的 `userId`。

### 8.1 推荐方案

初期可继续使用当前自定义 token + session 表。

后期可升级为：

1. JWT + Redis 黑名单
2. 或 OAuth2 Resource Server

对当前项目来说，建议先做：

1. 保留现有 token 机制
2. 将 token 校验从业务服务中抽到 `auth-service`
3. 通过 Gateway 转发用户上下文

## 9. 服务调用设计

### 9.1 product-service 对外接口

建议提供：

1. `GET /internal/products/{id}`
2. `POST /internal/products/batch`
3. `POST /internal/products/snapshot`

用途：

1. `order-service` 在创建订单时校验商品存在性和价格。
2. `knowledge-service` 初始化菜单知识库时同步商品信息。

### 9.2 auth-service 对外接口

建议提供：

1. `POST /internal/auth/validate`
2. `GET /internal/auth/user/{id}`

用途：

1. Gateway 校验 token。
2. 其他服务补充用户上下文。

### 9.3 user-service 对外接口

建议提供：

1. `GET /internal/users/{id}`
2. `GET /internal/users/{id}/default-address`
3. `GET /internal/users/{id}/preferences`

用途：

1. AI 服务获取用户画像。
2. 订单服务填充默认地址。

## 10. 分阶段实施路线

## 10.1 阶段一：单体模块化

目标：不拆进程，先拆代码边界。

实施内容：

1. Maven 改为多模块工程。
2. 抽出 `common-core`。
3. 抽出 `auth-module`、`user-module`、`product-module`、`order-module`、`ai-module`、`knowledge-module`。
4. 统一返回结构、异常码、日志规范。
5. 统一配置读取方式。
6. 清理跨模块直接调用 Repository 的代码。

交付结果：

1. 代码边界清晰。
2. 为服务拆分做好准备。
3. 不影响现有运行方式。

### 10.2 阶段二：引入 Gateway + Auth 独立服务

目标：先把入口和鉴权独立出来。

实施内容：

1. 新建 `api-gateway`
2. 新建 `auth-service`
3. 将登录、token、session 从单体迁出
4. 小程序与后台只访问 Gateway
5. Gateway 统一处理跨域、鉴权、路由

交付结果：

1. 所有客户端入口统一。
2. 后续拆分其他服务时前端改动最小。

### 10.3 阶段三：拆 AI 和 Knowledge

目标：先拆收益最高的服务。

实施内容：

1. 新建 `ai-service`
2. 新建 `knowledge-service`
3. 把 AI 问答、聊天记录、RAG 检索迁出
4. 上传文件改为独立存储目录或对象存储
5. 通过内部接口从 user/product 获取所需信息

交付结果：

1. AI 服务可独立扩容
2. 大模型故障不影响订单主链路
3. 知识库能力可独立演进

### 10.4 阶段四：拆 Order 和 Product

目标：建立核心业务服务边界。

实施内容：

1. 新建 `order-service`
2. 新建 `product-service`
3. 订单创建改为调用商品服务获取价格快照
4. 后台订单和菜单管理分别落到各自服务

交付结果：

1. 订单和商品边界明确
2. 可支持独立部署与扩容

### 10.5 阶段五：拆 User 服务与后台聚合层

目标：完成核心业务拆分。

实施内容：

1. 新建 `user-service`
2. 地址、收藏、偏好、用户信息迁出
3. 视需要增加 `admin-bff`

交付结果：

1. 核心业务服务完整成型
2. 后台查询走聚合接口

## 11. 当前仓库建议的改造顺序

结合当前代码，最推荐的顺序如下：

1. 先多模块化，不先拆服务。
2. 改造 token 鉴权，不再允许前端把 `userId` 作为可信身份。
3. 上 Gateway。
4. 独立 auth-service。
5. 独立 ai-service。
6. 独立 knowledge-service。
7. 独立 product-service。
8. 独立 order-service。
9. 最后独立 user-service 和后台聚合层。

这个顺序的原因：

1. 风险最低。
2. 前端改动可控。
3. AI 服务最容易成为资源热点，拆分收益最大。
4. 订单链路最敏感，放到中后期处理更稳。

## 12. 前端改造要求

### 12.1 小程序改造

当前小程序直接访问 `http://localhost:8081`，后续应改为访问 Gateway 地址，例如：

1. 开发环境：`http://localhost:8080`
2. 测试环境：`http://test-api.xxx.com`
3. 生产环境：`https://api.xxx.com`

改造要求：

1. `BASE_URL` 改成 Gateway 地址。
2. 所有请求统一由 Gateway 转发。
3. 前端不再显式信任 `userId`。
4. 上传接口走统一文件服务或 AI 服务网关路由。

### 12.2 管理后台改造

改造要求：

1. 保持 `baseURL=/api` 不变。
2. 由 Nginx 或 Vite 代理到 Gateway。
3. 后台鉴权可先保留 `X-Admin-Secret` 方案，后续升级为后台账号体系。

## 13. 配置中心改造

当前配置分散在 `application.yml` 和 `.env`，后续应迁移到配置中心。

建议拆分：

1. 公共配置
   - 日志级别
   - 跨域
   - 超时时间

2. 服务级配置
   - 数据源
   - Redis
   - 模型 API Key
   - 向量库地址
   - 微信配置
   - 高德配置

3. 环境级配置
   - dev
   - test
   - prod

注意：

1. API Key 不要继续硬编码在仓库配置里。
2. 所有敏感配置必须外置。

## 14. 监控与运维建议

微服务化后必须补足运维能力。

至少需要：

1. `Actuator`
2. 统一日志 traceId
3. 服务健康检查
4. 请求耗时监控
5. 错误率监控
6. AI 接口调用成功率监控
7. 网关限流监控

建议监控指标：

1. 登录成功率
2. 下单成功率
3. 订单状态变更成功率
4. AI 问答耗时
5. AI 外部接口失败率
6. RAG 检索命中率
7. 文件上传失败率

## 15. 关键风险与规避方案

### 15.1 风险一：服务拆分后订单一致性下降

表现：

1. 商品服务成功，订单服务失败
2. 商品价格变化导致订单金额不一致

规避：

1. 下单时生成商品快照
2. 避免跨服务分布式事务
3. 必要时使用消息队列实现最终一致性

### 15.2 风险二：鉴权混乱

表现：

1. 某些服务信任前端传 `userId`
2. 某些服务信任 token 解析结果

规避：

1. 统一由 Gateway/Auth 解析用户身份
2. 下游服务只读取可信请求头中的用户上下文

### 15.3 风险三：AI 服务拆分后文件与知识库状态不一致

表现：

1. 文件上传成功但索引失败
2. 文档删除后向量未清理

规避：

1. 文件状态、文档状态、索引状态分开记录
2. 增加异步重试机制
3. 增加补偿任务

### 15.4 风险四：后台统计接口变慢

表现：

1. 后台 dashboard 需要跨多个服务聚合数据

规避：

1. 通过 `admin-bff` 聚合
2. 统计类数据异步汇总
3. 使用 Redis 缓存热点统计

## 16. 第一版落地清单

如果现在开始实施，建议第一期只做以下内容：

1. 调整为 Maven 多模块工程
2. 抽出 `common-core`
3. 抽出 `auth-service`
4. 新增 `api-gateway`
5. 小程序和后台统一走 Gateway
6. 清理前端显式传 `userId` 的高风险接口
7. 将 AI 和 Knowledge 从主业务代码中逻辑解耦

第一期不要做：

1. 不要急着拆所有数据库
2. 不要急着引入 Seata
3. 不要急着做全量分布式事务
4. 不要急着把所有服务都拆出来

## 17. 第二版落地清单

在第一期稳定后，再做：

1. 独立 `ai-service`
2. 独立 `knowledge-service`
3. 接入 Redis 统一缓存
4. 接入 Nacos 配置中心
5. 接入服务注册发现
6. 接入链路追踪

## 18. 最终建议

对于本项目，最合理的技术路线不是“直接把 Spring Boot 改成全量 Spring Cloud”，而是：

1. 先模块化
2. 再网关化
3. 先拆鉴权
4. 优先拆 AI
5. 后拆订单与商品
6. 最后再考虑独立拆库与后台聚合

这样做的核心收益是：

1. 风险更低
2. 成本更可控
3. 更适合当前代码现状
4. 可以边改边运行，不需要一次性推倒重来

---

如果继续推进，下一份建议补充的文档是：

1. `cloud-服务拆分清单.md`
2. `cloud-数据库拆分方案.md`
3. `cloud-接口改造清单.md`
4. `cloud-迁移实施计划.md`

