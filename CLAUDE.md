# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WeChat Mini-program AI Customer Service + Ordering system. Spring Cloud microservices backend, WeChat mini-program frontend, Vue3 admin panel. AI chat powered by **Xiaomi MiMo** (`mimo-v2.5`, multimodal), with RAG knowledge base. JDK 17, Spring Boot 3.2, Spring Cloud 2023, Spring Cloud Alibaba, Nacos, MySQL 8.

## Build & Run

Multi-module Maven project. Parent pom at `backend/pom.xml`, 8 modules under `backend/`.

```bash
# Full build (all modules)
cd backend && mvn clean package -DskipTests

# Run a single service (IMPORTANT: spring-boot:run with -am fails — it runs the goal
# on the parent pom which has no main class. Install common-core first, then run without -am)
mvn install -pl common-core -am -DskipTests        # one-time: put shared dep in .m2
mvn spring-boot:run -pl ai-chat-service -DskipTests  # NO -am

# Run tests
mvn test                                           # all
mvn test -Dtest=TokenServiceImplTest -pl auth-service   # single test

# Admin panel (Vue3)
cd admin && npm install && npm run dev

# Start all services (requires .env + running Nacos)
powershell -File scripts/cloud/start-services.ps1

# Initialize database
mysql -u root -p < scripts/sql/init-database.sql
mysql -u root -p aiwechat < scripts/sql/aiwechat.sql
```

## Backend Architecture

```
Controller -> Service -> Repository -> Database
```

**Microservices** (each `backend/<module>`, port in parentheses):
- `common-core` — shared DTOs (`CommonResponse`, `UserContext`), exceptions, JWT util, interceptors. **Dependency for all services** (install to .m2 before running any service).
- `api-gateway` (9000) — Spring Cloud Gateway. `AuthTokenRelayFilter` validates token via auth-service and injects `X-User-Id`/`X-Open-Id`/`X-User-Nickname` headers for downstream services.
- `auth-service` (9091) — WeChat OAuth login, token issuance (72h expiry).
- `menu-service` (9092) — menu CRUD, favorites, image upload.
- `order-service` — orders + user addresses. Status flow: `pending` -> `confirmed` -> `preparing` -> `delivering` -> `completed` (or `cancelled`).
- `ai-chat-service` (9094) — AI chat, intent recognition, session memory, content safety. **See AI Chat Service below.**
- `knowledge-service` (9095) — RAG: LazyGraphRAG, hybrid search, query routing, eval, multi-strategy chunking (FAQ/Menu/Rule/Generic).
- `admin-service` — admin panel backend API.

**Cross-cutting**: services read user identity from gateway-injected headers via `UserContextHelper.requireUserId(httpRequest)` (header `X-User-Id`).

## AI Chat Service

**Provider**: Xiaomi MiMo (`https://api.xiaomimimo.com/v1`), single model `mimo-v2.5` (multimodal — supports image input). **No multi-model rotation** (previously ModelScope rotation caused `choices: null` issues; switched to single MiMo model).

**Critical: `thinking: {type: disabled}` must be sent in every chat request body.** MiMo defaults to reasoning mode (generates `reasoning_content`, slow + costly). All 5 chat call sites in `ai-chat-service` add this: `ModelScopeAICustomerServiceImpl` (main chat + image path), `IntentRecognitionServiceImpl`, `SessionMemoryServiceImpl`, `UserPreferenceServiceImpl`. (Class name retains "ModelScope" prefix for historical reasons — it now calls MiMo.)

**Retry/backoff** (`callModelScopeApi`): `maxAttempts = max(models.size(), chatMaxAttempts)` (config `chat.max-attempts`, default 3). Non-first attempts sleep `800ms * attempt`. **4xx errors (except 429) do NOT retry** — fail fast to avoid useless backoff. 5xx/429/network errors retry.

**Pipeline** (`handleUserQuery`): intent recognition + RAG retrieval run in parallel via `CompletableFuture` (15s/10s timeouts) -> route system prompt + 3-layer memory (facts/summary/recent) -> call MiMo -> update memory -> save record -> (human transfer currently disabled).

**Vision**: image path uses `mimo-v2.5` with OpenAI-compatible `image_url` content blocks.

`KnowledgeBaseServiceImpl` is separate — calls knowledge-service (HTTP, port 9095), not the chat API.

## Configuration Chain

**Runtime config flow**: `.env` -> `scripts/nacos/publish-config.ps1` renders `scripts/nacos/templates/*.tpl` (substituting `${VAR}`) -> publishes yaml to Nacos -> services import via `spring.config.import: optional:nacos:<dataId>.yaml`.

- `application.yml` imports `.env` then Nacos configs — **Nacos config overrides `.env`** (higher precedence).
- `spring.ai.openai.api-key` maps to env `MODELSCOPE_API_KEY` (name kept for `.tpl` compatibility; **holds the MiMo key**).
- `spring.ai.openai.base-url`/`chat.options.model`/`models` map to `AI_BASE_URL`/`AI_MODEL`/`AI_MODELS`.

**Key env vars** (see `.env.example`): `DB_*`, `WECHAT_APP_*`, `MODELSCOPE_API_KEY` (MiMo key), `AI_BASE_URL`, `AI_MODEL`, `AI_MODELS`, `EMBEDDING_API_KEY`/`EMBEDDING_BASE_URL` (智谱, separate from chat), `AMAP_API_KEY`, `NACOS_*`.

## Frontend

**Miniprogram** (`miniprogram/`): native WeChat. `utils/api.js` (endpoint modules), `utils/request.js` (HTTP client, injects `Authorization: Bearer <token>`, supports POST query params via 3rd arg `post(url, data, {param: value})`). Login: `wx.login` -> `POST /api/wechat/login` -> token in storage.

**Admin** (`admin/`): Vue3 + Vite + Element Plus + Pinia. `src/api/` clients, `src/views/` (dashboard/users/orders/menus/chat-records/feedback), `src/layouts/BasicLayout.vue`.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/wechat/login` | WeChat login with code |
| GET | `/api/wechat/user/info` | Get user info |
| GET | `/api/menu/items` | All available items |
| GET | `/api/menu/search?keyword=` | Search items |
| POST | `/api/orders` | Create order |
| GET | `/api/orders/user/{userId}` | User orders |
| POST | `/api/orders/{orderNumber}/cancel?userId=` | Cancel order |
| GET/POST/PUT/DELETE | `/api/addresses/*` | Address CRUD |
| GET/POST/DELETE | `/api/favorites/*` | Favorites |
| GET | `/api/locations/geocode?address=` | Amap geocode |
| POST | `/api/ai-chat/ask` | Ask AI (requires `X-User-Id` header) |
| GET | `/api/ai-chat/history/{userId}` | Chat history |
| GET | `/api/knowledge/documents` | List KB docs |
| POST | `/api/knowledge/sync` | Trigger KB sync |
| POST | `/api/feedback` | Submit feedback |
| GET/POST/PUT/DELETE | `/api/admin/*` | Admin panel APIs |

## Database

MySQL 8 at `localhost:3306/aiwechat` (root/123456 default). Key tables: `users`, `user_sessions`, `user_addresses`, `user_favorites`, `orders`, `order_items`, `menu_items`, `chat_records`, `knowledge_documents` (+ knowledge chunk/concept/edge tables for graph RAG). See `scripts/sql/`.

## Key Patterns & Gotchas

- **Token auth**: token in `wx.setStorageSync('token')`, injected as `Authorization: Bearer <token>` header; gateway validates + injects `X-User-Id`.
- **`ChatRequest.userId` is `@NotBlank`** — must be in the JSON body, not just the `X-User-Id` header (controller sets it from header, but `@Valid` runs before). Response field is `reply`, not `answer`.
- **`@RequestBody` vs `@RequestParam`**: JSON body -> `@RequestBody`; query params -> `@RequestParam`. Frontend `post()` 3rd arg passes query params.
- **Price display**: menu items use pre-processed `priceFormatted` field in WXML.
- **Nacos client snapshot**: when Nacos server is down, the Spring Cloud Alibaba client reads cached config from `~/nacos/config/fixed-<addr>_nacos/snapshot/<GROUP>/<dataId>.yaml`. If you change `.env` but the service still loads old config, **this snapshot is the culprit** — delete or update it.
- **Windows + non-ASCII in curl**: `curl -d '{"keywords":"中文"}'` transcodes Chinese to GBK -> HTTP 400. Use `printf '%s' '...' | curl --data-binary @-` or `-d @file` (raw UTF-8 bytes).
- **MSYS/encoding**: use `export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"` for Maven builds.
- **Lombok**: `@Slf4j`, `@Service`, `@RequiredArgsConstructor` (constructor injection) throughout.
- **MyBatis-Plus**: XML mappers in `resources/mapper/`; `map-underscore-to-camel-case: true`.

## Common Issues

- **`spring-boot:run -pl X -am` fails** ("no main class"): the `-am` adds parent to reactor and the run goal executes on parent. Fix: `mvn install -pl common-core -am` first, then `mvn spring-boot:run -pl X` (no `-am`).
- **Service loads old (ModelScope) config despite updated `.env`**: Nacos client snapshot cache (see above) OR Nacos server still has old config (re-run `publish-config.ps1`).
- **Missing tables**: run `scripts/sql/init-database.sql` + `scripts/sql/aiwechat.sql`.
- **Admin CORS**: backend must allow admin dev server origin.
- **Redis optional**: rate limiting + some cache features need Redis.

## References

- `AGENTS.md` — AI chat service critical facts (MiMo API behavior, retry, architecture notes)
- `README.md` — quick start, tech stack
- `docs/` — API docs, RAG setup guide, Nacos startup guide
