USE aiwechat;

-- 查看当前 menu_items 表的所有数据
SELECT id, item_id, name, category, price FROM menu_items ORDER BY id;

-- 如果 category 是数字，更新为分类名称
UPDATE menu_items SET category = '主食' WHERE category = '1' OR category = 1;
UPDATE menu_items SET category = '热菜' WHERE category = '2' OR category = 2;
UPDATE menu_items SET category = '素菜' WHERE category = '3' OR category = 3;
UPDATE menu_items SET category = '汤类' WHERE category = '4' OR category = 4;

-- 再次查看更新后的数据
SELECT id, item_id, name, category, price FROM menu_items ORDER BY id;

-- 查看不同的分类
SELECT DISTINCT category FROM menu_items;
