# AIWechat GraphRAG 升级实施计划书

## 1. 文档目的

本文档用于指导 **AIWechat 当前知识库体系** 从“传统向量 RAG”逐步演进到“增强版 RAG / 轻量结构化检索 / GraphRAG”的完整实施过程。

目标不是一次性重写现有知识库，而是：

1. 在 **不破坏现有客服能力** 的前提下持续提升检索质量
2. 优先解决当前项目中最真实、最高频的业务问题
3. 让后续 GraphRAG 建设具备清晰的阶段边界、交付物、风险控制和回滚方案

---

## 2. 当前项目现状

### 2.1 当前知识库链路

当前项目已经具备一条可工作的向量 RAG 链路：

1. `knowledge-service` 负责知识入库、分块、向量化、检索
2. `ai-chat-service` 在用户提问时先做 RAG 检索，再将结果注入 Prompt
3. 大模型基于检索结果、用户信息、会话记忆生成回复

### 2.2 当前核心实现位置

- 知识入库与检索实现：
  [KnowledgeBaseServiceImpl.java](D:/vis/aiwechat/aiwechat/backend/knowledge-service/src/main/java/com/aiwechat/knowledge/service/impl/KnowledgeBaseServiceImpl.java)
- AI 客服主流程：
  [ModelScopeAICustomerServiceImpl.java](D:/vis/aiwechat/aiwechat/backend/ai-chat-service/src/main/java/com/aiwechat/aichat/service/impl/ModelScopeAICustomerServiceImpl.java)
- 当前 RAG 使用说明：
  [RAG_SETUP_GUIDE.md](D:/vis/aiwechat/aiwechat/docs/RAG_SETUP_GUIDE.md)

### 2.3 当前能力边界

当前方案的优势：

- 接入成本低
- 适合 FAQ、菜单问答、价格查询、简单规则说明
- 与当前小程序客服场景兼容性高

当前方案的主要限制：

- 检索单位仍以文本 chunk 为中心
- 缺少“实体”和“关系”的显式建模
- 对多跳问题支持较弱
- 检索结果解释性有限
- 知识维护仍以文档/文本为主，不利于复杂运营规则治理

---

## 3. 为什么现在不建议直接一步到位上 GraphRAG

### 3.1 当前业务特点

AIWechat 当前知识问题主要集中在：

- 菜品介绍
- 价格查询
- 配送说明
- 下单与订单问题
- 常见客服 FAQ
- 门店/服务规则说明

这些问题多数仍属于：

- 单跳检索
- 半结构化规则问答
- 基于当前订单/地址/菜单数据的上下文回答

### 3.2 GraphRAG 真正适合的场景

只有当系统里频繁出现以下问题时，GraphRAG 的收益才会明显放大：

- 需要跨多个实体做关系链推理
- 需要回答“谁影响谁”“哪几个对象互相关联”
- 需要做跨文档、多实体、多约束聚合
- 需要解释“为什么命中这些知识”

例如：

- 某套餐包含哪些菜品，这些菜品里哪些包含过敏原
- 某个用户历史偏好如何影响推荐
- 某条运营规则会影响哪些门店、哪些分类、哪些配送区域
- 某次投诉涉及订单、门店、配送时段、客服会话之间的关联

### 3.3 当前阶段更合适的策略

最合理的路线不是“直接上 GraphRAG”，而是：

1. 先把现有向量 RAG 做扎实
2. 再补轻量结构化检索层
3. 等确实出现稳定的多跳需求后，再引入 GraphRAG

---

## 4. 目标架构

### 4.1 三阶段目标

#### 阶段 A：增强版向量 RAG

目标：

- 提升现有检索质量
- 降低无关召回
- 建立可评估、可迭代的检索体系

主要能力：

- 更合理的 chunk 策略
- 更完整的 metadata
- 混合检索
- 重排序
- 查询改写
- 检索效果评估

#### 阶段 B：轻量结构化检索层

目标：

- 将强结构化业务知识从“纯文本知识”中拆出来
- 让菜单、分类、价格、配送规则、门店信息、营业规则具备可编排查询能力

主要能力：

- 菜单/门店/规则实体建模
- 结构化索引
- 规则优先命中
- 向量检索与结构化检索融合

#### 阶段 C：GraphRAG

目标：

- 让系统具备实体关系推理能力
- 支持复杂多跳问答
- 支持知识社区摘要与图路径召回

主要能力：

- 实体抽取
- 关系抽取
- 图存储
- 图遍历
- 图摘要
- 图 + 向量联合召回

---

## 5. 目标业务范围

### 5.1 第一批纳入 GraphRAG/结构化建模的知识对象

建议优先建模以下实体：

- 菜品 `MenuItem`
- 菜品分类 `Category`
- 套餐 `Combo`
- 配料/标签 `Ingredient / Tag`
- 过敏原 `Allergen`
- 门店 `Store`
- 配送区域 `DeliveryArea`
- 订单状态规则 `OrderRule`
- 优惠活动 `Promotion`
- 常见问题 `FAQ`

建议优先建模以下关系：

- 菜品 属于 分类
- 套餐 包含 菜品
- 菜品 包含 配料
- 菜品 关联 过敏原
- 门店 提供 菜品
- 门店 覆盖 配送区域
- 优惠活动 适用 菜品/分类/门店
- FAQ 关联 订单阶段 / 门店 / 配送规则

### 5.2 暂不建议第一阶段纳入的对象

以下内容暂时不建议一开始就纳入 GraphRAG：

- 完整聊天记录全文图谱化
- 用户长期行为图谱化
- 全量订单事件图谱化
- 多模态图片知识图谱

原因很简单：数据量、清洗成本、建模复杂度都明显更高，会拖慢首轮落地。

---

## 6. 分阶段实施计划

## 6.1 阶段 A：增强版向量 RAG

### 6.1.1 目标

在不引入图数据库的前提下，把当前传统 RAG 提升到“生产可优化”的水平。

### 6.1.2 当前问题

从现有实现看，当前知识库仍存在以下典型问题：

- `SimpleVectorStore`/内存向量存储约束明显
- 文档粒度较粗，chunk 规则统一且静态
- metadata 维度偏少
- 检索只做单路向量相似度 top-K
- 对召回质量缺少评估指标
- 文档清单、删除、清空等治理能力较弱

### 6.1.3 具体任务

#### 任务 A1：重构知识切分策略

交付内容：

- 文档类型分层切分策略
- FAQ、菜单、规则、说明文档分别使用不同 chunk 模板
- 统一 chunk metadata 结构

建议新增 metadata：

- `documentId`
- `source`
- `documentType`
- `bizType`
- `category`
- `menuId`
- `storeId`
- `updatedAt`
- `version`

#### 任务 A2：建立知识入库治理

交付内容：

- 文档入库记录表
- chunk 元信息落库
- 文档版本控制
- 重复入库检测
- 增量更新机制

建议新增表：

- `knowledge_documents`
- `knowledge_chunks`
- `knowledge_ingest_tasks`

#### 任务 A3：增加混合检索

交付内容：

- 向量召回
- 关键词召回
- metadata 过滤
- 召回结果融合排序

建议优先支持：

- 菜品名强匹配
- 分类词强匹配
- 业务关键词白名单
- 用户 query 重写后再召回

#### 任务 A4：增加重排序

交付内容：

- 粗召回 topN
- rerank topK
- 输出最终知识上下文

收益：

- 降低“语义相似但业务无关”的噪音
- 提高菜单问答和规则问答准确率

#### 任务 A5：建立评估闭环

交付内容：

- 测试问集
- 标准答案 / 标准证据集
- Top1 / Top3 命中率
- 无答案率
- 错召率

建议建立评测集分类：

- 菜单问答
- 价格问答
- 配送规则
- 订单规则
- 售后 FAQ

### 6.1.4 阶段 A 输出

- 增强版知识入库链路
- 混合检索能力
- 基础评测体系
- 更稳定的 RAG Prompt 注入策略

### 6.1.5 预估周期

- 2 到 3 周

---

## 6.2 阶段 B：轻量结构化检索层

### 6.2.1 目标

把适合结构化查询的业务知识从“文档检索”中剥离出来，形成“结构化查询优先，向量检索兜底”的双通道体系。

### 6.2.2 为什么这一层很重要

很多客服问题本质上不是“语义检索”问题，而是“结构化取数”问题。

例如：

- 某菜品多少钱
- 某分类有哪些推荐菜
- 配送范围覆盖哪些区域
- 当前订单状态能不能取消
- 某优惠活动对哪些商品有效

这类问题如果继续强行走纯文本 RAG，会出现：

- 能答，但不稳定
- 可解释性差
- 更新成本高
- 精确值容易漂移

### 6.2.3 具体任务

#### 任务 B1：定义结构化知识模型

建议先建以下对象：

- 菜品知识视图
- 配送规则视图
- 门店营业规则视图
- 优惠规则视图

#### 任务 B2：增加结构化检索服务

建议新增模块或服务层：

- `StructuredKnowledgeService`
- `RuleQueryService`
- `KnowledgeRouterService`

职责：

- 判断 query 是否适合结构化查询
- 结构化命中后直接产出 evidence
- 未命中再进入向量 RAG

#### 任务 B3：改造 AI 客服检索入口

当前 [ModelScopeAICustomerServiceImpl.java](D:/vis/aiwechat/aiwechat/backend/ai-chat-service/src/main/java/com/aiwechat/aichat/service/impl/ModelScopeAICustomerServiceImpl.java) 在提问时直接调用：

- `knowledgeBaseService.search(question, 3)`

阶段 B 建议改造成：

1. 先做 query routing
2. 再走结构化检索 / 向量检索 / 双路融合
3. 最后统一构造 evidence context

#### 任务 B4：输出统一证据格式

建议定义统一 evidence 结构：

```json
{
  "type": "structured | vector | graph",
  "source": "menu_item | rule | faq | graph_path",
  "title": "证据标题",
  "content": "证据正文",
  "score": 0.92,
  "metadata": {}
}
```

### 6.2.4 阶段 B 输出

- 结构化知识层
- Query 路由能力
- 双通道检索架构
- 更高精度的菜单/规则问答

### 6.2.5 预估周期

- 2 到 4 周

---

## 6.3 阶段 C：GraphRAG

### 6.3.1 目标

在前两阶段打稳之后，正式引入图谱层，为复杂多跳推理提供底座。

### 6.3.2 GraphRAG 架构建议

建议采用以下四层结构：

1. 原始知识层
   - 文档
   - FAQ
   - 菜单数据
   - 规则数据
2. 抽取层
   - 实体抽取
   - 关系抽取
   - 社区聚类
   - 摘要生成
3. 存储层
   - 图数据库
   - 向量数据库
   - 结构化关系表
4. 检索层
   - 图路径召回
   - 社区摘要召回
   - 语义检索召回
   - 结果融合与重排序

### 6.3.3 技术选型建议

#### 图存储

候选方案：

- Neo4j
- NebulaGraph
- JanusGraph

现阶段建议优先：

- **Neo4j**

原因：

- 上手成本低
- 社区成熟
- Cypher 查询可读性高
- 适合第一阶段图谱 PoC

#### 向量存储

当前项目仍偏轻量，建议阶段 A/B 先升级现有向量层，再视规模迁移：

- Redis Vector
- pgvector
- Milvus

#### 实体关系抽取

建议采用“两段式”：

1. 规则抽取
2. LLM 补充抽取

不要一开始就完全依赖 LLM 全自动抽取，否则成本和脏数据风险都会偏高。

### 6.3.4 图谱建设范围

第一批建议只覆盖这几类图：

- 菜单图
- 配送规则图
- 订单规则图
- FAQ 语义关系图

第二批再考虑：

- 用户偏好图
- 投诉与售后事件图
- 门店运营图

### 6.3.5 具体任务

#### 任务 C1：定义图谱 schema

需要明确：

- 节点类型
- 关系类型
- 属性字段
- 主键规则
- 更新时间策略

#### 任务 C2：建设抽取流水线

流程建议：

1. 原始文本清洗
2. 分段
3. 实体抽取
4. 关系抽取
5. 去重与归一化
6. 写入图数据库
7. 生成社区摘要
8. 建立图节点/摘要的向量索引

#### 任务 C3：建设图检索器

建议新增：

- `GraphKnowledgeService`
- `GraphQueryPlanner`
- `GraphEvidenceBuilder`

职责：

- 识别 query 涉及哪些实体
- 规划图查询路径
- 产出图证据和摘要证据

#### 任务 C4：建设 GraphRAG Router

路由策略建议：

- 精确问价 / 菜单查询 -> 结构化
- FAQ / 说明性问答 -> 向量 RAG
- 关系链 / 影响分析 / 多约束问题 -> GraphRAG

#### 任务 C5：建设图谱评估

评估指标建议：

- 实体抽取准确率
- 关系抽取准确率
- 图路径命中率
- 多跳问题正确率
- 平均响应时延
- 单次 query 成本

### 6.3.6 阶段 C 输出

- 图谱 schema
- 图谱构建流水线
- 图查询服务
- 图 + 向量联合召回
- GraphRAG PoC

### 6.3.7 预估周期

- 4 到 8 周

---

## 7. 推荐实施顺序

### 7.1 总原则

不建议跳过阶段 A 和阶段 B 直接做阶段 C。

推荐顺序：

1. 阶段 A：增强版 RAG
2. 阶段 B：结构化检索层
3. 阶段 C：GraphRAG PoC
4. 阶段 C 扩容到生产能力

### 7.2 建议里程碑

| 里程碑 | 目标 | 交付物 |
|---|---|---|
| M1 | 当前 RAG 稳定化 | 混合检索、metadata、评测集 |
| M2 | 结构化知识接入 | Query Router、规则查询、统一 evidence |
| M3 | GraphRAG PoC | Neo4j 图谱、实体关系抽取、图路径召回 |
| M4 | 生产化 | 图谱更新任务、监控、回滚、成本控制 |

---

## 8. 对现有代码的改造建议

## 8.1 `knowledge-service` 改造点

当前 [KnowledgeBaseServiceImpl.java](D:/vis/aiwechat/aiwechat/backend/knowledge-service/src/main/java/com/aiwechat/knowledge/service/impl/KnowledgeBaseServiceImpl.java) 建议重点改造：

### 第一批改造

- 抽离 `DocumentIngestService`
- 抽离 `ChunkStrategy`
- 抽离 `MetadataBuilder`
- 抽离 `RetrievalService`

### 第二批改造

- 增加文档持久化
- 增加 chunk 索引表
- 增加结构化知识导入能力

### 第三批改造

- 增加图谱构建任务
- 增加图谱同步任务
- 增加图查询接口

## 8.2 `ai-chat-service` 改造点

当前 [ModelScopeAICustomerServiceImpl.java](D:/vis/aiwechat/aiwechat/backend/ai-chat-service/src/main/java/com/aiwechat/aichat/service/impl/ModelScopeAICustomerServiceImpl.java) 里建议重点改造：

### 第一批改造

- 将 `knowledgeBaseService.search(question, 3)` 抽成统一检索入口
- 新增 `QueryRoutingService`
- 新增 `EvidenceAssembler`

### 第二批改造

- Prompt 构造中区分：
  - 结构化 evidence
  - 向量 evidence
  - 图谱 evidence

### 第三批改造

- 增加多路 evidence 融合策略
- 增加 answer trace 能力
- 增加回答来源解释

---

## 9. 数据与存储设计建议

### 9.1 建议新增的数据对象

- `knowledge_documents`
- `knowledge_chunks`
- `knowledge_entities`
- `knowledge_relations`
- `knowledge_graph_sync_tasks`
- `knowledge_eval_cases`
- `knowledge_eval_results`

### 9.2 图数据库中的核心节点

- `MenuItem`
- `Category`
- `Ingredient`
- `Allergen`
- `Store`
- `DeliveryRule`
- `Promotion`
- `Faq`

### 9.3 图数据库中的核心边

- `BELONGS_TO`
- `CONTAINS`
- `HAS_ALLERGEN`
- `AVAILABLE_AT`
- `APPLIES_TO`
- `RELATED_TO`
- `RESTRICTED_BY`

---

## 10. 接口规划建议

### 10.1 后台知识管理接口

建议新增以下管理接口：

- `/api/admin/knowledge/reindex`
- `/api/admin/knowledge/eval/run`
- `/api/admin/knowledge/graph/rebuild`
- `/api/admin/knowledge/graph/status`
- `/api/admin/knowledge/entities`
- `/api/admin/knowledge/relations`

### 10.2 AI 检索调试接口

建议新增调试接口：

- `/api/admin/ai/retrieve/debug`
- `/api/admin/ai/query/route`
- `/api/admin/ai/graph/path/debug`

作用：

- 查看 query 被路由到哪条链路
- 查看召回结果
- 查看 evidence 组合方式

---

## 11. 测试与验收计划

## 11.1 单元测试

需要覆盖：

- chunk 切分策略
- metadata 构建
- query routing
- 结构化检索
- 图谱 schema 映射
- 图查询构造

## 11.2 集成测试

需要覆盖：

- 文档导入 -> 检索 -> 回复
- 菜单结构化查询 -> 回复
- 图谱构建 -> 图查询 -> 回复

## 11.3 评测集

建议至少准备：

- 100 条 FAQ 问题
- 100 条菜单问答
- 50 条订单规则问答
- 50 条多跳关系问题

## 11.4 验收标准

阶段 A 验收：

- Top3 命中率显著优于当前版本
- 错召率下降
- 关键问答稳定性提升

阶段 B 验收：

- 菜单/规则类问题优先命中结构化证据
- 回复一致性明显提升

阶段 C 验收：

- 多跳问题正确率达到可接受水平
- 图谱更新链路可稳定执行

---

## 12. 风险与应对

| 风险 | 描述 | 应对策略 |
|---|---|---|
| 建模过度 | 过早把简单问题图谱化，投入过大 | 先做结构化层，再做 GraphRAG |
| 抽取噪音高 | 实体关系抽取错误导致图谱污染 | 规则抽取优先，LLM 抽取做辅助 |
| 成本过高 | 抽取、摘要、重排都依赖模型调用 | 分阶段灰度启用，限制批任务频率 |
| 响应变慢 | 多路召回和图查询增加时延 | 路由前置，缓存热点 query |
| 难以运维 | 图谱任务、图数据库、评测链路复杂 | 从 PoC 开始，不要一次性铺满 |

---

## 13. 资源评估

### 13.1 人力

建议最少投入：

- 后端工程师 1 到 2 人
- AI / 检索工程师 1 人
- 测试 / 评测支持 1 人

### 13.2 基础设施

阶段 A：

- 现有 MySQL + 当前向量存储即可启动

阶段 B：

- 需要额外结构化索引和任务调度能力

阶段 C：

- 需要图数据库
- 需要批处理任务
- 需要额外模型调用预算

---

## 14. 最终建议

结合 AIWechat 当前业务形态，推荐路线如下：

### 短期建议

先做 **增强版 RAG**，不要直接重构成完整 GraphRAG。

### 中期建议

补 **结构化知识检索层**，优先解决菜单、价格、配送、规则类问答。

### 长期建议

在确实出现稳定的多跳问题场景后，再建设 **GraphRAG PoC**，并优先围绕：

- 套餐关系
- 菜品配料/过敏原
- 门店与配送范围
- 订单规则与售后 FAQ

### 一句话结论

**你这个项目适合“分阶段演进到 GraphRAG”，不适合“立即全面 GraphRAG 化”。**

---

## 15. 建议的下一步动作

建议立即执行以下 5 项工作：

1. 盘点现有知识源，按 FAQ / 菜单 / 规则 / 门店四类整理
2. 给当前知识入库增加 metadata 和文档治理
3. 建立 200 到 300 条客服评测集
4. 在 `ai-chat-service` 中增加 query routing 抽象
5. 输出一版“结构化知识模型设计稿”

---

*文档版本：1.0*  
*创建时间：2026-04-24*  
*适用项目：AIWechat 多服务版（miniprogram + admin + Spring Cloud backend）*
