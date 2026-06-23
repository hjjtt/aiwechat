-- =============================================
-- aiwechat 数据库初始化脚本
-- 在新环境部署时执行: mysql -u root -p < schema.sql
-- =============================================

CREATE DATABASE IF NOT EXISTS aiwechat DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aiwechat;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_id VARCHAR(100) NOT NULL UNIQUE,
    nickname VARCHAR(100),
    avatar_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户会话表
CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(200) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at DATETIME,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 菜品表
CREATE TABLE IF NOT EXISTS menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    image_url VARCHAR(500),
    is_available TINYINT(1) DEFAULT 1,
    sales_count INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    total_amount DECIMAL(10,2) NOT NULL,
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    delivery_address TEXT,
    remark TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    INDEX idx_user_id (user_id),
    INDEX idx_order_number (order_number),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单项表
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    menu_id BIGINT,
    menu_name VARCHAR(100),
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户地址表
CREATE TABLE IF NOT EXISTS user_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    address TEXT NOT NULL,
    detail_address VARCHAR(200),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    is_default TINYINT(1) DEFAULT 0,
    label VARCHAR(20),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户收藏表
CREATE TABLE IF NOT EXISTS user_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_menu (user_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 聊天记录表
CREATE TABLE IF NOT EXISTS chat_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    session_id VARCHAR(100),
    question TEXT,
    answer TEXT,
    sources JSON,
    role VARCHAR(20),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识库文档表
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200),
    content TEXT,
    source VARCHAR(256),
    file_path VARCHAR(500),
    file_type VARCHAR(20),
    biz_type VARCHAR(64),
    category VARCHAR(128),
    status VARCHAR(32) DEFAULT 'active',
    version INT DEFAULT 1,
    chunk_count INT DEFAULT 0,
    metadata TEXT,
    content_hash VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_kd_content_hash (content_hash),
    INDEX idx_kd_biz_type (biz_type),
    INDEX idx_kd_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识库分块表
CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id VARCHAR(64) PRIMARY KEY,
    document_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    chunk_index INT NOT NULL DEFAULT 0,
    chunk_type VARCHAR(32),
    source VARCHAR(256),
    vector_synced TINYINT(1) DEFAULT 0,
    keywords VARCHAR(512),
    metadata TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_kc_document_id (document_id),
    INDEX idx_kc_chunk_type (chunk_type),
    INDEX idx_kc_vector_synced (vector_synced)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识库评测用例表
CREATE TABLE IF NOT EXISTS knowledge_eval_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(512) NOT NULL,
    expected_answer TEXT,
    expected_source VARCHAR(256),
    category VARCHAR(32),
    difficulty VARCHAR(16) DEFAULT 'easy',
    enabled TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_kec_category (category),
    INDEX idx_kec_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识库评测结果表
CREATE TABLE IF NOT EXISTS knowledge_eval_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    eval_case_id BIGINT,
    question VARCHAR(512),
    search_type VARCHAR(32),
    top1_hit TINYINT(1),
    top3_hit TINYINT(1),
    top_result TEXT,
    top_result_source VARCHAR(256),
    top_score DOUBLE,
    cost_ms BIGINT,
    result_count INT,
    evaluated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ker_eval_case_id (eval_case_id),
    INDEX idx_ker_evaluated_at (evaluated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户偏好表
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    UNIQUE KEY uk_user_key (user_id, preference_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 反馈表
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(100),
    question TEXT,
    ai_answer TEXT,
    rating INT NOT NULL,
    comment TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
