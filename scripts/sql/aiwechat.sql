/*
 Navicat Premium Data Transfer

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80041 (8.0.41)
 Source Host           : localhost:3306
 Source Schema         : aiwechat

 Target Server Type    : MySQL
 Target Server Version : 80041 (8.0.41)
 File Encoding         : 65001

 Date: 05/03/2026 15:43:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chat_records
-- ----------------------------
DROP TABLE IF EXISTS `chat_records`;
CREATE TABLE `chat_records`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `question` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `sources` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_records
-- ----------------------------
INSERT INTO `chat_records` VALUES (1, '1', '3b52b7ec-a7bc-40f1-a45b-49688d0e330a', '起送价格', NULL, NULL, '2026-03-05 19:03:23', 'user');
INSERT INTO `chat_records` VALUES (2, '1', '3b52b7ec-a7bc-40f1-a45b-49688d0e330a', NULL, '嗨！欢迎来到小餐，想知道起送价格吗？我们一般起送价是19.9元，下单满这个金额就可以享受优惠啦！如果你还没下单，可以先看看有没有合适的菜品，或者告诉我你想要的口味和数量，我来帮你推荐哦～', NULL, '2026-03-05 19:03:24', 'assistant');
INSERT INTO `chat_records` VALUES (3, '1', 'e689095c-2d61-4ffa-bb2a-63b96a7c91a8', '起送价格', NULL, NULL, '2026-03-05 19:03:23', 'user');
INSERT INTO `chat_records` VALUES (4, '1', 'e689095c-2d61-4ffa-bb2a-63b96a7c91a8', NULL, '你好呀！起送价格是咱们小程序的优惠政策哦，具体是多少呢？我们这边给您来个简单说明：\n\n- 如果您下单数量达到**5件及以上**，就可以享受**免配送费**啦！\n- 如果下单不足5件，就需要支付**基础配送费**，具体金额会根据您的订单情况来定。\n\n如果您有具体的订单信息，比如订单数、待付款状态等，我可以帮您更准确地计算起送价格哦！\n\n想看看有没有优惠活动吗？或者需要我帮您推荐几道好菜？', NULL, '2026-03-05 19:03:24', 'assistant');
INSERT INTO `chat_records` VALUES (5, '1', 'cc5287f5-3cba-4f0f-85cd-25b55a36c936', '你好呀', NULL, NULL, '2026-03-05 19:03:31', 'user');
INSERT INTO `chat_records` VALUES (6, '1', 'cc5287f5-3cba-4f0f-85cd-25b55a36c936', NULL, '你好呀！欢迎来到小餐～😊\n\n我们这边起送价是19.9元，下单满这个金额就可以享受优惠啦！如果你还没下单，可以先看看有没有合适的菜品，或者告诉我你想要的口味和数量，我来帮你推荐哦～\n\n另外，咱们小程序有几项小福利：\n- 下单**5件及以上**，可以免配送费！\n- 不足5件的话，需要支付基础配送费，具体金额会根据订单情况来定。\n\n如果你想了解优惠活动，或者需要我帮你推荐几道好菜，随时告诉我哦～😊', NULL, '2026-03-05 19:03:32', 'assistant');
INSERT INTO `chat_records` VALUES (7, '1', '82d2927e-54d1-4796-90c0-6ef4a9dd58ce', '你知道1+1等于几吗', NULL, NULL, '2026-03-05 19:04:06', 'user');
INSERT INTO `chat_records` VALUES (8, '1', '82d2927e-54d1-4796-90c0-6ef4a9dd58ce', NULL, '嗨！你知道1+1等于几吗？哈哈，这个问题超简单哦！1加1等于2呀～ 😄\n\n我们小程序的起送价是19.9元，下单满这个金额就可以享受优惠啦！如果你还没下单，可以先看看有没有合适的菜品，或者告诉我你想要的口味和数量，我来帮你推荐哦～\n\n另外，咱们小程序有几项小福利：\n- 下单**5件及以上**，可以免配送费！\n- 不足5件的话，需要支付基础配送费，具体金额会根据订单情况来定。\n\n如果你想了解优惠活动，或者需要我帮你推荐几道好菜，随时告诉我哦～😊', NULL, '2026-03-05 19:04:07', 'assistant');
INSERT INTO `chat_records` VALUES (9, '1', '5aaf5e19-e241-4e58-a4a9-e4b46ea4d7f8', '优惠活动', NULL, NULL, '2026-03-05 19:27:42', 'user');
INSERT INTO `chat_records` VALUES (10, '1', '3252dabf-bbea-4281-8cd2-21114e8f60a1', '优惠活动', NULL, NULL, '2026-03-05 19:27:42', 'user');
INSERT INTO `chat_records` VALUES (11, '1', '5aaf5e19-e241-4e58-a4a9-e4b46ea4d7f8', NULL, '嗨！优惠活动超多哦，来看看有哪些啦～ 😊\n\n1. **满减活动**：下单满19.9元可以享受优惠，还能叠加其他折扣哦！  \n2. **5件免配送**：只要订单数量达到5件及以上，就可以免配送费啦！  \n3. **赠品活动**：有时候我们会推出赠品、小礼包之类的福利，记得关注我们的推送哦～  \n4. **限时优惠**：比如节日促销、生日优惠等，都是特别的活动，随时关注我们的通知！\n\n想了解具体的优惠活动吗？或者需要我帮你推荐几道好菜？随时告诉我哦～😊', NULL, '2026-03-05 19:27:43', 'assistant');
INSERT INTO `chat_records` VALUES (12, '1', '3252dabf-bbea-4281-8cd2-21114e8f60a1', NULL, '嗨！优惠活动超多哦～来看看有哪些啦！\n\n- **满减活动**：下单满19.9元即可享受优惠，还能叠加其他折扣哦！\n- **5件以上免配送费**：只要订单数量达到5件及以上，就可以免配送费啦！\n- **赠品活动**：有时候会有赠品或者小礼物，比如买菜送调料包之类的，记得关注我们的推送哦！\n- **会员福利**：成为会员可以享受更多专属优惠，比如积分兑换、生日礼包等。\n\n想了解具体的优惠活动吗？或者需要我帮你推荐几道好菜？随时告诉我呀～😊', NULL, '2026-03-05 19:27:43', 'assistant');

-- ----------------------------
-- Table structure for knowledge_documents
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_documents`;
CREATE TABLE `knowledge_documents`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文档内容',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件路径',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件类型',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库文档表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_documents
-- ----------------------------

-- ----------------------------
-- Table structure for menu_items
-- ----------------------------
DROP TABLE IF EXISTS `menu_items`;
CREATE TABLE `menu_items`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `price` decimal(10, 2) NOT NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `is_available` tinyint NULL DEFAULT 1,
  `sales_count` int NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `item_id`(`item_id` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_is_available`(`is_available` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of menu_items
-- ----------------------------
INSERT INTO `menu_items` VALUES (1, 'ITEM001', '招牌牛肉面', '选用上等牛肉，面条手工制作，汤头浓郁', 38.00, '主食', NULL, 1, 156, '2026-03-05 11:02:13', '2026-03-05 11:02:13');
INSERT INTO `menu_items` VALUES (2, 'ITEM002', '宫保鸡丁', '经典川菜，鸡肉嫩滑，花生香脆', 32.00, '热菜', NULL, 1, 89, '2026-03-05 11:02:13', '2026-03-05 11:02:13');
INSERT INTO `menu_items` VALUES (3, 'ITEM003', '蒜蓉西兰花', '清淡营养，蒜香浓郁', 18.00, '素菜', NULL, 1, 67, '2026-03-05 11:02:13', '2026-03-05 11:02:13');
INSERT INTO `menu_items` VALUES (4, 'ITEM004', '红烧肉', '五花肉肥而不腻，入口即化', 45.00, '热菜', NULL, 1, 234, '2026-03-05 11:02:13', '2026-03-05 11:02:13');
INSERT INTO `menu_items` VALUES (5, 'ITEM005', '酸辣汤', '开胃爽口，汤色金黄', 12.00, '汤类', NULL, 1, 45, '2026-03-05 11:02:13', '2026-03-05 11:02:13');
INSERT INTO `menu_items` VALUES (6, 'ITEM006', '蛋炒饭', '粒粒分明，香气扑鼻', 18.00, '主食', NULL, 1, 178, '2026-03-05 11:02:13', '2026-03-05 11:02:13');

-- ----------------------------
-- Table structure for order_items
-- ----------------------------
DROP TABLE IF EXISTS `order_items`;
CREATE TABLE `order_items`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  `menu_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(10, 2) NOT NULL,
  `subtotal` decimal(10, 2) NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_items
-- ----------------------------
INSERT INTO `order_items` VALUES (1, 1, 5, '酸辣汤', 1, 12.00, 12.00, '2026-03-05 19:37:20');

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending',
  `total_amount` decimal(10, 2) NULL DEFAULT NULL,
  `delivery_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_number`(`order_number` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES (1, 'ORD20260305888D8D', 1, 'completed', 12.00, '湖南省湘潭市', 'autumn', '13813813888', '', '2026-03-05 19:37:20', '2026-03-05 11:39:57');

-- ----------------------------
-- Table structure for user_addresses
-- ----------------------------
DROP TABLE IF EXISTS `user_addresses`;
CREATE TABLE `user_addresses`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系电话',
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省份',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '城市',
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区县',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '完整地址',
  `detail_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细地址',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '经度',
  `is_default` tinyint NULL DEFAULT 0 COMMENT '是否默认地址',
  `label` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址标签',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_addresses
-- ----------------------------
INSERT INTO `user_addresses` VALUES (1, 1, 'autumn', '13813813888', NULL, NULL, NULL, '湖南省湘潭市', '', 27.8563200, 112.9073700, 1, '家', '2026-03-05 11:34:59', '2026-03-05 11:34:59');
INSERT INTO `user_addresses` VALUES (2, 1, 'autumn', '15107520294', NULL, NULL, NULL, '湖南省湘潭市', '', 27.8563200, 112.9073700, 0, '家', '2026-03-05 11:51:02', '2026-03-05 11:51:02');

-- ----------------------------
-- Table structure for user_favorites
-- ----------------------------
DROP TABLE IF EXISTS `user_favorites`;
CREATE TABLE `user_favorites`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_menu`(`user_id` ASC, `menu_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_favorites
-- ----------------------------

-- ----------------------------
-- Table structure for user_sessions
-- ----------------------------
DROP TABLE IF EXISTS `user_sessions`;
CREATE TABLE `user_sessions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `last_active_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `token`(`token` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_token`(`token` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_sessions
-- ----------------------------
INSERT INTO `user_sessions` VALUES (1, 1, 'fcf98da9e76f4e6db8081c2e19321789', '2026-03-08 19:03:09', '2026-03-05 19:03:09', '2026-03-05 19:03:09');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '微信用户OpenID',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'active' COMMENT '状态: active=正常, banned=禁用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `open_id`(`open_id` ASC) USING BTREE,
  INDEX `idx_open_id`(`open_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'oBIgH7KYU_DNvW4Uac3b9zvOyYJw', 'autumn', 'http://tmp/GshCsbI-CCnKe16acc670338149eacc69129f0b46e7b.jpeg', 'active', '2026-03-05 19:03:09', '2026-03-05 19:03:09');

-- ----------------------------
-- Table structure for feedback
-- ----------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '会话ID',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '用户问题',
  `ai_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'AI回答',
  `rating` int NOT NULL COMMENT '评分 1-5',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '用户评论',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id` (`user_id`) USING BTREE,
  INDEX `idx_rating` (`rating`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
