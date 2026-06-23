-- ============================================
-- 数据库索引优化 - 提升查询性能
-- ============================================

USE aiwechat;

-- ============================================
-- 聊天记录表索引优化
-- ============================================

-- 复合索引用于快速查询用户的最近对话
DROP INDEX IF EXISTS idx_user_session_created ON chat_records;
CREATE INDEX idx_user_session_created ON chat_records (user_id, created_at DESC);

-- 角色索引用于快速过滤用户或助手消息
DROP INDEX IF EXISTS idx_role ON chat_records;
CREATE INDEX idx_role ON chat_records (role);

-- ============================================
-- 订单表索引优化
-- ============================================

-- 复合索引用于快速查询用户订单和状态
DROP INDEX IF EXISTS idx_user_status_created ON orders;
CREATE INDEX idx_user_status_created ON orders (user_id, status, created_at DESC);

-- ============================================
-- 用户会话表索引优化
-- ============================================

-- 复合索引用于快速查找过期的会话
DROP INDEX IF EXISTS idx_user_expires ON user_sessions;
CREATE INDEX idx_user_expires ON user_sessions (user_id, expires_at);

-- ============================================
-- 订单明细表索引优化
-- ============================================

-- 复合索引用于快速查询订单明细
DROP INDEX IF EXISTS idx_order_menu ON order_items;
CREATE INDEX idx_order_menu ON order_items (order_id, menu_id);

-- ============================================
-- 菜单表索引优化
-- ============================================

-- 复合索引用于快速查询可用菜品和分类
DROP INDEX IF EXISTS idx_category_available ON menus;
CREATE INDEX idx_category_available ON menus (category, is_available, sales_count DESC);

-- ============================================
-- 用户收藏表索引优化
-- ============================================

-- 复合索引用于快速查询用户收藏
DROP INDEX IF EXISTS idx_user_created ON user_favorites;
CREATE INDEX idx_user_created ON user_favorites (user_id, created_at DESC);

-- ============================================
-- 用户地址表索引优化
-- ============================================

-- 复合索引用于快速查询用户默认地址
DROP INDEX IF EXISTS idx_user_default ON user_addresses;
CREATE INDEX idx_user_default ON user_addresses (user_id, is_default DESC, created_at DESC);

-- ============================================
-- 表分区建议 (大数据量时使用)
-- ============================================

-- 对于 chat_records 表，如果数据量超过百万级，可以考虑按时间分区
-- ALTER TABLE chat_records PARTITION BY RANGE (YEAR(created_at)*100 + MONTH(created_at)) (
--     PARTITION p202401 VALUES LESS THAN (202402),
--     PARTITION p202402 VALUES LESS THAN (202403),
--     ...
-- );

-- ============================================
-- 查询性能分析建议
-- ============================================

-- 启用查询日志（开发环境使用，生产环境谨慎开启）
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 2;

-- 定期分析表
-- ANALYZE TABLE chat_records;
-- ANALYZE TABLE orders;
-- ANALYZE TABLE order_items;

-- 定期优化表
-- OPTIMIZE TABLE chat_records;
-- OPTIMIZE TABLE orders;
