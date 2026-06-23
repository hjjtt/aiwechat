-- =============================================
-- AIWechat 知识库增强 - 阶段A 数据库变更
-- =============================================

-- 1. 增强 knowledge_documents 表（基于现有结构添加字段）
ALTER TABLE knowledge_documents
    ADD COLUMN source VARCHAR(256) COMMENT '来源标识' AFTER content,
    ADD COLUMN biz_type VARCHAR(64) COMMENT '业务类型：menu, delivery, order_rule, faq, promotion' AFTER file_type,
    ADD COLUMN category VARCHAR(128) COMMENT '分类标签' AFTER biz_type,
    ADD COLUMN status VARCHAR(32) DEFAULT 'active' COMMENT '状态：active, deleted' AFTER category,
    ADD COLUMN version INT DEFAULT 1 COMMENT '文档版本号' AFTER status,
    ADD COLUMN chunk_count INT DEFAULT 0 COMMENT '分块数量' AFTER version,
    ADD COLUMN metadata TEXT COMMENT '扩展元数据（JSON）' AFTER chunk_count,
    ADD COLUMN content_hash VARCHAR(64) COMMENT '内容哈希（用于去重）' AFTER metadata;

-- 为已有数据设置默认值
UPDATE knowledge_documents SET status = 'active', version = 1 WHERE status IS NULL;

-- 添加索引
CREATE INDEX idx_kd_content_hash ON knowledge_documents(content_hash);
CREATE INDEX idx_kd_biz_type ON knowledge_documents(biz_type);
CREATE INDEX idx_kd_status ON knowledge_documents(status);

-- 2. 新建 knowledge_chunks 表
CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键ID',
    document_id BIGINT NOT NULL COMMENT '所属文档ID',
    content TEXT NOT NULL COMMENT 'chunk 文本内容',
    chunk_index INT NOT NULL DEFAULT 0 COMMENT 'chunk 在文档中的顺序',
    chunk_type VARCHAR(32) COMMENT 'chunk 类型：faq, menu, rule, description',
    source VARCHAR(256) COMMENT '来源标识',
    vector_synced TINYINT(1) DEFAULT 0 COMMENT '向量是否已同步：0未同步, 1已同步',
    keywords VARCHAR(512) COMMENT '关键词（逗号分隔）',
    metadata TEXT COMMENT '扩展元数据（JSON）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_kc_document_id (document_id),
    INDEX idx_kc_chunk_type (chunk_type),
    INDEX idx_kc_vector_synced (vector_synced),
    FULLTEXT INDEX ft_kc_content (content) WITH PARSER ngram,
    FULLTEXT INDEX ft_kc_keywords (keywords) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分块索引表';

-- 3. 新建评测用例表
CREATE TABLE IF NOT EXISTS knowledge_eval_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(512) NOT NULL COMMENT '测试问题',
    expected_answer TEXT COMMENT '标准答案',
    expected_source VARCHAR(256) COMMENT '期望命中的来源标识',
    category VARCHAR(32) COMMENT '问题分类：menu, price, delivery, order_rule, faq, multi_hop',
    difficulty VARCHAR(16) DEFAULT 'easy' COMMENT '难度：easy, medium, hard',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_kec_category (category),
    INDEX idx_kec_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库评测用例';

-- 4. 新建评测结果表
CREATE TABLE IF NOT EXISTS knowledge_eval_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    eval_case_id BIGINT COMMENT '评测用例ID',
    question VARCHAR(512) COMMENT '测试问题',
    search_type VARCHAR(32) COMMENT '检索类型',
    top1_hit TINYINT(1) COMMENT 'Top1是否命中',
    top3_hit TINYINT(1) COMMENT 'Top3是否命中',
    top_result TEXT COMMENT '第一条结果内容',
    top_result_source VARCHAR(256) COMMENT '第一条结果来源',
    top_score DOUBLE COMMENT '检索得分',
    cost_ms BIGINT COMMENT '耗时(ms)',
    result_count INT COMMENT '返回结果数',
    evaluated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ker_eval_case_id (eval_case_id),
    INDEX idx_ker_evaluated_at (evaluated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库评测结果';

-- 5. LazyGraphRAG 轻量概念图：索引阶段不调用 LLM，仅存储概念、共现边与 chunk 关联
CREATE TABLE IF NOT EXISTS knowledge_concepts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL COMMENT '概念原始名称',
    normalized_name VARCHAR(128) NOT NULL COMMENT '归一化概念名称',
    concept_type VARCHAR(32) DEFAULT 'concept' COMMENT '概念类型：menu, rule, faq, concept',
    frequency INT DEFAULT 1 COMMENT '出现次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_kc_normalized_name (normalized_name),
    INDEX idx_kc_type (concept_type),
    INDEX idx_kc_frequency (frequency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LazyGraphRAG 概念表';

CREATE TABLE IF NOT EXISTS knowledge_concept_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    concept_id BIGINT NOT NULL COMMENT '概念ID',
    chunk_id VARCHAR(64) NOT NULL COMMENT 'chunk ID',
    document_id BIGINT COMMENT '文档ID',
    weight DOUBLE DEFAULT 1.0 COMMENT '概念在 chunk 中的权重',
    source VARCHAR(256) COMMENT 'chunk 来源',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_kcc_concept_chunk (concept_id, chunk_id),
    INDEX idx_kcc_concept_id (concept_id),
    INDEX idx_kcc_chunk_id (chunk_id),
    INDEX idx_kcc_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LazyGraphRAG 概念与 chunk 关联表';

CREATE TABLE IF NOT EXISTS knowledge_concept_edges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_concept_id BIGINT NOT NULL COMMENT '起点概念ID',
    target_concept_id BIGINT NOT NULL COMMENT '终点概念ID',
    edge_type VARCHAR(32) DEFAULT 'co_occurs' COMMENT '边类型',
    weight INT DEFAULT 1 COMMENT '共现权重',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_kce_pair (source_concept_id, target_concept_id, edge_type),
    INDEX idx_kce_source (source_concept_id),
    INDEX idx_kce_target (target_concept_id),
    INDEX idx_kce_weight (weight)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LazyGraphRAG 概念共现边表';
