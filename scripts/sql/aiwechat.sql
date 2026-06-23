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
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `role` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'admin',
  `status` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `createdAt` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `Admin_username_key`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin
-- ----------------------------

-- ----------------------------
-- Table structure for aimessage
-- ----------------------------
DROP TABLE IF EXISTS `aimessage`;
CREATE TABLE `aimessage`  (
  `id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sessionId` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tokens` int NULL DEFAULT NULL,
  `createdAt` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `AIMessage_sessionId_fkey`(`sessionId` ASC) USING BTREE,
  CONSTRAINT `AIMessage_sessionId_fkey` FOREIGN KEY (`sessionId`) REFERENCES `aisession` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of aimessage
-- ----------------------------

-- ----------------------------
-- Table structure for aisession
-- ----------------------------
DROP TABLE IF EXISTS `aisession`;
CREATE TABLE `aisession`  (
  `id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `userId` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `createdAt` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `AISession_userId_fkey`(`userId` ASC) USING BTREE,
  CONSTRAINT `AISession_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of aisession
-- ----------------------------

-- ----------------------------
-- Table structure for cart
-- ----------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart`  (
  `id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `userId` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `productId` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` int NOT NULL DEFAULT 1,
  `selected` tinyint(1) NOT NULL DEFAULT 1,
  `createdAt` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `Cart_userId_productId_key`(`userId` ASC, `productId` ASC) USING BTREE,
  INDEX `Cart_productId_fkey`(`productId` ASC) USING BTREE,
  CONSTRAINT `Cart_productId_fkey` FOREIGN KEY (`productId`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `Cart_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cart
-- ----------------------------

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `parentId` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `sortOrder` int NOT NULL DEFAULT 0,
  `icon` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `createdAt` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `Category_parentId_fkey`(`parentId` ASC) USING BTREE,
  CONSTRAINT `Category_parentId_fkey` FOREIGN KEY (`parentId`) REFERENCES `category` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of category
-- ----------------------------
INSERT INTO `category` VALUES ('cat-1', '电子产品', NULL, 1, NULL, '2026-02-20 08:36:52.939', '2026-02-20 08:36:52.939');
INSERT INTO `category` VALUES ('cat-2', '服装鞋包', NULL, 2, NULL, '2026-02-20 08:36:52.939', '2026-02-20 08:36:52.939');
INSERT INTO `category` VALUES ('cat-3', '食品生鲜', NULL, 3, NULL, '2026-02-20 08:36:52.939', '2026-02-20 08:36:52.939');
INSERT INTO `category` VALUES ('cat-4', '家居用品', NULL, 4, NULL, '2026-02-20 08:36:52.939', '2026-02-20 08:36:52.939');

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
-- Table structure for order
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order`  (
  `id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `orderNo` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `userId` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `totalAmount` decimal(10, 2) NOT NULL,
  `payAmount` decimal(10, 2) NOT NULL,
  `status` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `payTime` datetime(3) NULL DEFAULT NULL,
  `shipTime` datetime(3) NULL DEFAULT NULL,
  `completeTime` datetime(3) NULL DEFAULT NULL,
  `cancelTime` datetime(3) NULL DEFAULT NULL,
  `cancelReason` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `shippingAddress` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `remark` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `createdAt` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `Order_orderNo_key`(`orderNo` ASC) USING BTREE,
  INDEX `Order_userId_fkey`(`userId` ASC) USING BTREE,
  CONSTRAINT `Order_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order
-- ----------------------------

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
-- Table structure for orderitem
-- ----------------------------
DROP TABLE IF EXISTS `orderitem`;
CREATE TABLE `orderitem`  (
  `id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `orderId` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `productId` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `productName` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `productImage` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `price` decimal(10, 2) NOT NULL,
  `quantity` int NOT NULL,
  `createdAt` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `OrderItem_orderId_fkey`(`orderId` ASC) USING BTREE,
  INDEX `OrderItem_productId_fkey`(`productId` ASC) USING BTREE,
  CONSTRAINT `OrderItem_orderId_fkey` FOREIGN KEY (`orderId`) REFERENCES `order` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `OrderItem_productId_fkey` FOREIGN KEY (`productId`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orderitem
-- ----------------------------

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
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `categoryId` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `price` decimal(10, 2) NOT NULL,
  `originalPrice` decimal(10, 2) NULL DEFAULT NULL,
  `stock` int NOT NULL DEFAULT 0,
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `status` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'onsale',
  `sales` int NOT NULL DEFAULT 0,
  `createdAt` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `Product_categoryId_fkey`(`categoryId` ASC) USING BTREE,
  CONSTRAINT `Product_categoryId_fkey` FOREIGN KEY (`categoryId`) REFERENCES `category` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES ('prod-1', 'iPhone 15 Pro Max', 'Apple iPhone 15 Pro Max，256GB，原钛金属设计，A17 Pro芯片，全新专业相机系统。', 'cat-1', 9999.00, 10999.00, 100, '[\"https://picsum.photos/800/800?random=1\",\"https://picsum.photos/800/800?random=2\"]', 'onsale', 523, '2026-02-20 08:36:52.960', '2026-02-20 08:36:52.960');
INSERT INTO `product` VALUES ('prod-10', 'iPad Pro 12.9寸', 'Apple iPad Pro 12.9英寸，M2芯片，Liquid Retina XDR显示屏。', 'cat-1', 9299.00, 9999.00, 60, '[\"https://picsum.photos/800/800?random=11\"]', 'onsale', 178, '2026-02-20 08:36:52.999', '2026-02-20 08:36:52.999');
INSERT INTO `product` VALUES ('prod-2', 'MacBook Pro 14寸', 'Apple MacBook Pro 14英寸，M3 Pro芯片，18GB统一内存，512GB固态硬盘。', 'cat-1', 15999.00, 16999.00, 50, '[\"https://picsum.photos/800/800?random=3\"]', 'onsale', 234, '2026-02-20 08:36:52.968', '2026-02-20 08:36:52.968');
INSERT INTO `product` VALUES ('prod-3', 'AirPods Pro 第二代', 'Apple AirPods Pro (第二代)，主动降噪，空间音频，MagSafe充电盒。', 'cat-1', 1899.00, 1999.00, 200, '[\"https://picsum.photos/800/800?random=4\"]', 'onsale', 892, '2026-02-20 08:36:52.974', '2026-02-20 08:36:52.974');
INSERT INTO `product` VALUES ('prod-4', 'Nike Air Max 270', 'Nike男子运动鞋，Air Max 270大气垫，舒适缓震，时尚百搭。', 'cat-2', 899.00, 1099.00, 150, '[\"https://picsum.photos/800/800?random=5\"]', 'onsale', 456, '2026-02-20 08:36:52.978', '2026-02-20 08:36:52.978');
INSERT INTO `product` VALUES ('prod-5', '优衣库联名款T恤', '优衣库联名款UT，纯棉舒适，多色可选，简约时尚。', 'cat-2', 79.00, 99.00, 500, '[\"https://picsum.photos/800/800?random=6\"]', 'onsale', 1234, '2026-02-20 08:36:52.981', '2026-02-20 08:36:52.981');
INSERT INTO `product` VALUES ('prod-6', '新西兰奇异果', '新西兰进口奇异果，12个装，富含维生素C，新鲜甜美。', 'cat-3', 68.00, 88.00, 300, '[\"https://picsum.photos/800/800?random=7\"]', 'onsale', 678, '2026-02-20 08:36:52.985', '2026-02-20 08:36:52.985');
INSERT INTO `product` VALUES ('prod-7', '三文鱼刺身', '新鲜三文鱼刺身拼盘，500g，挪威进口，-18°C冷链配送。', 'cat-3', 168.00, 198.00, 80, '[\"https://picsum.photos/800/800?random=8\"]', 'onsale', 234, '2026-02-20 08:36:52.989', '2026-02-20 08:36:52.989');
INSERT INTO `product` VALUES ('prod-8', '戴森吸尘器V15', 'Dyson V15 Detect无线手持吸尘器，激光探测，强劲吸力。', 'cat-4', 4990.00, 5490.00, 30, '[\"https://picsum.photos/800/800?random=9\"]', 'onsale', 89, '2026-02-20 08:36:52.993', '2026-02-20 08:36:52.993');
INSERT INTO `product` VALUES ('prod-9', '美的智能电饭煲', '美的智能电饭煲，4L大容量，IH电磁加热，多功能烹饪。', 'cat-4', 399.00, 499.00, 120, '[\"https://picsum.photos/800/800?random=10\"]', 'onsale', 345, '2026-02-20 08:36:52.996', '2026-02-20 08:36:52.996');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `openid` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `unionid` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `nickname` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `avatar` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `email` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `password` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `isAdmin` tinyint(1) NOT NULL DEFAULT 0,
  `status` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `createdAt` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `User_openid_key`(`openid` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES ('6822e5fb-eb22-41c6-a783-4564901c7249', 'mock_openid_admin_admin_code', NULL, 'admin', NULL, NULL, NULL, NULL, 0, 'active', '2026-02-20 08:38:22.413', '2026-02-20 08:38:22.413');
INSERT INTO `user` VALUES ('user-admin-1', NULL, NULL, '管理员', 'https://picsum.photos/200/200?random=101', NULL, 'admin@example.com', NULL, 1, 'active', '2026-02-20 08:36:53.009', '2026-02-20 08:36:53.009');
INSERT INTO `user` VALUES ('user-test-1', NULL, NULL, '测试用户', 'https://picsum.photos/200/200?random=100', '13800138000', NULL, NULL, 0, 'active', '2026-02-20 08:36:53.004', '2026-02-20 08:36:53.004');

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

SET FOREIGN_KEY_CHECKS = 1;
