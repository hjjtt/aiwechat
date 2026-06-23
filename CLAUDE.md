# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WeChat Mini-program AI Customer Service System with Spring Boot backend. Features AI-powered menu ordering, order management, and intelligent chat support using ModelScope API (Qwen3-1.7B model).

## Build Commands

```bash
# Backend - compile and package
mvn clean package -DskipTests

# Backend - run directly
java -jar target/aiwechat-1.0.0-SNAPSHOT.jar

# Backend - run with Maven
mvn spring-boot:run

# Backend - run tests
mvn test
mvn test -Dtest=TokenServiceImplTest  # Run specific test

# Database initialization (MySQL)
mysql -u root -p < src/main/resources/schema.sql

# Admin panel (Vue.js)
cd admin && npm install && npm run dev
```

## Backend Architecture

```
Controller → Service → Repository → Database
```

**Key Controllers:**
- `WechatController` (`/api/wechat`) - Login/logout/user info
- `AIChatController` (`/api/ai-chat`) - AI chat Q&A with RAG knowledge base
- `OrderController` (`/api/orders`) - Order CRUD
- `MenuController` (`/api/menu`) - Menu items
- `AddressController` (`/api/addresses`) - User addresses
- `FavoriteController` (`/api/favorites`) - Favorite items
- `LocationController` (`/api/locations`) - Amap geocoding
- `KnowledgeController` (`/api/knowledge`) - Knowledge base management
- `FeedbackController` (`/api/feedback`) - User feedback
- `AdminController` (`/api/admin`) - Health/metrics
- `AdminUserController`, `AdminOrderController`, `AdminMenuController`, `AdminChatRecordController` - Admin panel APIs

**Key Services:**
- `WechatLoginService` - WeChat OAuth flow, token generation
- `ModelScopeAICustomerServiceImpl` - AI chat via ModelScope API (multi-model rotation: Qwen3-1.7B, GLM-4.7-Flash, Kimi-K2.5, M2.5)
- `TokenServiceImpl` - Session token management (72h expiry)
- `OrderServiceImpl` - Order creation, status management, cancellation
- `KnowledgeBaseServiceImpl` - RAG knowledge base with PDF document support
- `AsyncAiServiceImpl` - Async AI processing with conversation caching
- `HumanTransferService` - Human agent transfer for complex queries
- `PerformanceMonitorService` - Performance metrics tracking
- `FeedbackService` - User feedback collection
- `AddressService` - User address CRUD
- `FavoriteService` - Favorite menu items management
- `LocationService` - Amap geocoding/reverse-geocoding

**Order Status Flow:**
`pending` → `confirmed` → `preparing` → `delivering` → `completed` (or `cancelled`)

**AI Configuration:**
- Base URL: `https://api-inference.modelscope.cn/v1`
- Default Model: `qwen/Qwen3-1.7B`
- Multi-model rotation: Qwen3-1.7B, GLM-4.7-Flash, Kimi-K2.5, M2.5
- Temperature: 0.7
- RAG Knowledge Base: PDF document embedding support
- Async processing with conversation caching
- Human transfer for complex queries

## Frontend Architecture

```
miniprogram/                    # WeChat Mini-program (customer facing)
├── app.js                      # App entry with auth check
├── utils/
│   ├── api.js                 # API endpoints module exports
│   ├── request.js             # HTTP client with auth & POST query params
│   └── location.js            # Location services (Amap)
└── pages/
    ├── login/                  # WeChat login page
    ├── index/                  # Home with quick actions
    ├── chat/                   # AI chat interface
    ├── menu/                   # Menu browsing with cart
    ├── orders/                 # Order list with status tabs
    ├── checkout/               # Checkout with item selection
    ├── profile/                # User profile
    ├── address-manage/         # Address management
    ├── location-picker/         # Location picker
    └── favorites/              # Favorite items
```

```
admin/                         # Vue.js Admin Panel (staff facing)
├── src/
│   ├── api/                   # API clients (auth, users, orders, menus, chat-records)
│   ├── views/                 # Dashboard, users, orders, menus, chat-records
│   ├── layouts/               # BasicLayout with sidebar navigation
│   └── stores/                # Pinia state management (user)
```

**API Utilities (`utils/api.js`):**
- `aiChat` - AI chat endpoints
- `knowledge` - Knowledge base search
- `menu` - Menu CRUD
- `order` - Order management
- `wechat` - Login/logout

**Login Flow:**
1. `wx.login()` → get code
2. `POST /api/wechat/login` with code
3. Backend returns token
4. Store token in `wx.setStorageSync('token')`
5. Include token in `Authorization: Bearer <token>` header

## Database Schema

Key tables: `users`, `user_sessions`, `user_addresses`, `user_favorites`, `orders`, `order_items`, `menu_items`, `chat_records`, `knowledge_documents`

**users table:**
- `open_id` - WeChat unique identifier (unique constraint)
- `nickname`, `avatar_url`, `status`, `created_at`, `last_login_at`

**user_sessions table:**
- `token` - Login token (unique)
- `user_id`, `expires_at`, `last_active_at`

**user_addresses table:**
- `user_id`, `contact_name`, `contact_phone`, `province`, `city`, `district`
- `address`, `detail_address`, `latitude`, `longitude`, `is_default`, `label`

**user_favorites table:**
- `user_id`, `menu_id` (unique composite key)

**orders table:**
- `id`, `order_number` (unique), `user_id`, `status`, `total_amount`
- `contact_name`, `contact_phone`, `delivery_address`, `remark`
- `created_at`, `updated_at`

**order_items table:**
- `id`, `order_id`, `menu_id`, `menu_name`, `quantity`, `unit_price`, `subtotal`

**menu_items table:**
- `item_id` (unique), `name`, `description`, `price`, `category`, `image_url`, `is_available`, `sales_count`

**chat_records table:**
- `user_id`, `session_id`, `question`, `answer`, `sources` (JSON), `role` (user/assistant)

**knowledge_documents table:**
- `title`, `content`, `file_path`, `file_type`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| **WeChat** |||
| POST | `/api/wechat/login` | WeChat login with code |
| POST | `/api/wechat/logout` | Logout |
| GET | `/api/wechat/user/info` | Get user info |
| **Menu** |||
| GET | `/api/menu/items` | Get all available items |
| GET | `/api/menu/category/{category}` | Get items by category |
| GET | `/api/menu/search?keyword=` | Search items |
| **Orders** |||
| POST | `/api/orders` | Create order |
| GET | `/api/orders/user/{userId}` | Get user orders |
| GET | `/api/orders/user/{userId}/status/{status}` | Get by status |
| POST | `/api/orders/{orderNumber}/cancel?userId=` | Cancel order |
| POST | `/api/orders/{orderNumber}/status?status=` | Update status |
| **Addresses** |||
| GET | `/api/addresses/user/{userId}` | Get user addresses |
| POST | `/api/addresses` | Create address |
| PUT | `/api/addresses/{id}` | Update address |
| DELETE | `/api/addresses/{id}` | Delete address |
| POST | `/api/addresses/{id}/default` | Set default address |
| **Favorites** |||
| GET | `/api/favorites/user/{userId}` | Get user favorites |
| POST | `/api/favorites` | Add favorite |
| DELETE | `/api/favorites/user/{userId}/menu/{menuId}` | Remove favorite |
| **Location** |||
| GET | `/api/locations/geocode?address=` | Geocode address |
| GET | `/api/locations/reverse-geocode?latitude=&longitude=` | Reverse geocode |
| GET | `/api/locations/search?keyword=&location=` | Search nearby places |
| **AI Chat** |||
| POST | `/api/ai-chat/ask` | Ask AI question |
| GET | `/api/ai-chat/history/{userId}` | Get chat history |
| POST | `/api/ai-chat/transfer` | Transfer to human agent |
| **Knowledge Base** |||
| GET | `/api/knowledge/documents` | List documents |
| POST | `/api/knowledge/documents` | Add document |
| POST | `/api/knowledge/sync` | Trigger knowledge sync |
| **Feedback** |||
| POST | `/api/feedback` | Submit feedback |
| **Admin** |||
| GET | `/api/admin/users` | List users (paginated) |
| GET | `/api/admin/orders` | List orders (paginated) |
| PUT | `/api/admin/orders/{orderNumber}/status` | Update order status |
| GET | `/api/admin/menus` | List menu items |
| POST | `/api/admin/menus` | Create menu item |
| PUT | `/api/admin/menus/{id}` | Update menu item |
| DELETE | `/api/admin/menus/{id}` | Delete menu item |
| GET | `/api/admin/chat-records` | List chat records |
| GET | `/api/admin/health` | Health check |
| GET | `/api/admin/metrics` | Performance metrics |

## Key Patterns

1. **Token Auth**: Store in Storage, inject via request interceptor
2. **Map-based DTOs**: RequestBody uses `Map<String, Object>` for flexibility
3. **Lombok**: `@Slf4j` for logging, `@Service`/`@RestController` annotations
4. **MyBatis**: XML mappers in `resources/mapper/`
5. **POST with Query Params**: Use third parameter in `post(url, data, {param: value})`

## Configuration

**application.yml:**
- Database: MySQL at localhost:3306/aiwechat (root/123456)
- Server port: 8081
- WeChat: `appId`, `appSecret`, `sessionHost`
- AI: `spring.ai.openai.api-key`, `base-url`, model settings
- Amap: `amap.api-key` for geocoding
- Rate limiting: 60 requests/minute (configurable)
- Token expiry: 72 hours (configurable)

**Environment Variables:**
- `MODELSCOPE_API_KEY` - ModelScope API key
- `WECHAT_APP_ID`, `WECHAT_APP_SECRET` - WeChat credentials
- `AMAP_API_KEY` - Amap API key
- `DATASOURCE_URL`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD`

**Frontend config (`miniprogram/utils/request.js`):**
- `BASE_URL`: Backend API address (http://localhost:8081)

**Admin config (`admin/src/api/request.js`):**
- `BASE_URL`: Backend API address

## Common Issues

- **@RequestParam vs @RequestBody**: Use `@RequestBody` for POST with JSON body, `@RequestParam` for query params
- **Lombok in MSYS**: Use `export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"`
- **Missing tables**: Run schema.sql before starting backend
- **Token type mismatch**: userInfo uses `Map<String, Object>` not `Map<String, String>`
- **POST query params**: Frontend `post()` function supports third param for query params: `post(url, data, {param: value})`
- **Price serialization**: Menu items use `priceFormatted` field for display in WXML (pre-processed in JS)
- **Admin panel CORS**: Ensure backend allows cross-origin requests from admin dev server
- **Redis dependency**: Rate limiting and some cache features require Redis to be enabled
