-- 更新 menu_items 表的 category 字段
-- category: 1=主食, 2=热菜, 3=素菜, 4=汤类

UPDATE menu_items SET category = '主食' WHERE category = '1' OR category = 1;
UPDATE menu_items SET category = '热菜' WHERE category = '2' OR category = 2;
UPDATE menu_items SET category = '素菜' WHERE category = '3' OR category = 3;
UPDATE menu_items SET category = '汤类' WHERE category = '4' OR category = 4;

-- 查看更新后的数据
SELECT id, item_id, name, category FROM menu_items;
