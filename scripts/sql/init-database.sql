-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS aiwechat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE aiwechat;

-- 更新 menu_items 表的 category 字段为正确的分类名称
UPDATE menu_items SET category = '主食' WHERE category = '1' OR category = 1;
UPDATE menu_items SET category = '热菜' WHERE category = '2' OR category = 2;
UPDATE menu_items SET category = '素菜' WHERE category = '3' OR category = 3;
UPDATE menu_items SET category = '汤类' WHERE category = '4' OR category = 4;

-- 查看更新后的数据
SELECT id, item_id, name, category FROM menu_items;
