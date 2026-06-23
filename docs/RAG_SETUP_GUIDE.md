# RAG 知识库快速启用指南

## ✅ 已完成的修改

### 1. 配置文件更新

**application.yml**
```yaml
spring:
  ai:
    openai:
      api-key: ${EMBEDDING_API_KEY}
      base-url: ${EMBEDDING_BASE_URL:https://open.bigmodel.cn/api/paas/v4}
      embedding:
        options:
          model: ${EMBEDDING_MODEL:embedding-3}
  
  # 向量存储配置（RAG）
  vectorstore:
    path: ${VECTORSTORE_PATH:./uploaded/vectorstore.json}
```

### 2. AiConfig.java 更新
- ✅ 使用 `SimpleVectorStore.builder(embeddingModel).build()` 创建向量存储
- ✅ `knowledge-service` 仅依赖 embedding / vector store，不再强依赖 chat bean
- ✅ 内存模式（Spring AI 1.1.2 持久化 API 有变化，后续版本添加）

### 3. ModelScopeAICustomerServiceImpl.java 更新
- ✅ 注入 KnowledgeBaseService
- ✅ 在 handleUserQuery 中添加 RAG 检索逻辑
- ✅ 添加 buildRagPrompt 方法构建增强 Prompt
- ✅ 有图片时跳过 RAG（直接使用多模态模型）

### 4. .env.example 更新
- ✅ 添加 EMBEDDING_MODEL 配置
- ✅ 添加 VECTORSTORE_PATH 配置

---

## 🚀 使用步骤

### Step 1: 创建 .env 文件（如果没有）

```bash
cd D:\vis\aiwechat\aiwechat
copy .env.example .env
```

编辑 `.env` 文件，确认以下配置：
```
EMBEDDING_API_KEY=your_embedding_api_key_here
EMBEDDING_BASE_URL=https://open.bigmodel.cn/api/paas/v4
EMBEDDING_MODEL=embedding-3
VECTORSTORE_PATH=./uploaded/vectorstore.json
```

### Step 2: 启动应用

```bash
# 方式 1: 使用 Maven
mvn spring-boot:run

# 方式 2: 打包后运行
mvn clean package -DskipTests
java -jar target/aiwechat-1.0.0-SNAPSHOT.jar
```

### Step 3: 初始化知识库

启动应用后，调用管理端知识库初始化接口：

```bash
# 使用 curl
curl -X POST http://localhost:9090/api/admin/knowledge/init-default ^
  -H "X-Admin-Secret: <admin_jwt_token>"
```

预期日志输出：
```
INFO  开始初始化默认知识库
INFO  默认知识库初始化完成: menu=..., deliveryRule=..., orderRule=..., faq=...
```

### Step 4: 测试 RAG 效果

**方式 1: 使用小程序聊天界面**
- 打开微信小程序
- 进入客服聊天
- 提问菜品相关问题，例如：
  - "宫保鸡丁多少钱？"
  - "有什么辣的菜推荐？"
  - "招牌牛肉面怎么样？"

**方式 2: 使用 API 测试**

```bash
curl -X POST http://localhost:8081/api/ai-chat/ask ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":\"1\",\"question\":\"宫保鸡丁多少钱？\",\"nickname\":\"测试用户\"}"
```

预期响应：
```json
{
  "success": true,
  "data": {
    "reply": "宫保鸡丁的价格是 XX 元...",
    "sources": ["宫保鸡丁"],
    "sessionId": "xxx"
  }
}
```

---

## 📊 验证 RAG 是否工作

### 查看日志

启动应用后，观察以下日志：

```
# RAG 检索日志
DEBUG 执行 RAG 知识库检索：宫保鸡丁多少钱？
INFO  RAG 检索到 3 条相关知识

# 或者（如果没有相关知识）
DEBUG RAG 检索无相关知识
```

### ⚠️ 注意事项

**当前版本使用内存向量存储**，重启应用后向量数据会丢失，需要重新调用 `/api/admin/knowledge/init-default` 初始化知识库。

持久化功能将在后续 Spring AI 版本升级后添加。

---

## 🔧 故障排查

### 问题 1: Embedding 模型调用失败

**症状**: 日志显示 Embedding 相关错误

**解决**:
1. 确认 EMBEDDING_API_KEY 配置正确
2. 检查网络连接
3. 确认 embedding 配置已切到智谱 OpenAI 兼容接口：
   ```
   EMBEDDING_BASE_URL=https://open.bigmodel.cn/api/paas/v4
   EMBEDDING_MODEL=embedding-3
   ```

### 问题 2: 知识库检索结果为空

**症状**: 总是显示"RAG 检索无相关知识"

**解决**:
1. 确认已调用 `/api/admin/knowledge/init-default` 初始化知识库
2. 检查数据库中是否有菜品数据：
   ```sql
   SELECT COUNT(*) FROM menu_items WHERE is_available = 1;
   ```
3. 手动添加测试知识：
   ```bash
   curl -X POST "http://localhost:9090/api/admin/knowledge/add-text?title=测试&content=这是测试内容" ^
     -H "X-Admin-Secret: <admin_jwt_token>"
   ```

### 问题 3: 向量存储文件未生成

**症状**: 应用关闭后没有生成 vectorstore.json

**解决**:
1. 检查 `uploaded/` 目录是否有写入权限
2. 查看应用日志是否有保存错误
3. 确认 AiConfig.java 中的 @PreDestroy 方法被调用

---

## 📈 优化建议

### 调整检索参数

在 `KnowledgeBaseServiceImpl.java` 中：

```java
private static final int DEFAULT_TOP_K = 5;        // 增加返回更多结果
private static final int CHUNK_SIZE = 500;         // 调整分块大小
private static final int CHUNK_OVERLAP = 100;      // 调整重叠大小
```

### 添加更多知识源

修改 `initMenuKnowledge()` 方法，添加：
- 优惠活动信息
- 配送说明
- 常见问题 FAQ

### 监控检索效果

在 `KnowledgeBaseServiceImpl.java` 的 search 方法中添加：
```java
log.info("RAG 检索 - query: {}, 结果数：{}, 平均分数：{}", 
    query, 
    chunkResults.size(), 
    chunkResults.stream().mapToDouble(ChunkResult::getScore).average().orElse(0));
```

---

## 🎯 下一步优化

1. **混合检索**: 向量检索 + 关键词检索
2. **查询重写**: 使用 LLM 优化用户查询
3. **结果重排序**: 使用 Cross-Encoder 重排序
4. **向量数据库**: 迁移到 Redis Vector / ChromaDB
5. **检索评估**: 收集用户反馈优化检索质量

---

**文档更新日期**: 2026-04-01
