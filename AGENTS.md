# AGENTS.md

## Project Overview

WeChat Mini-program AI customer service system: Spring Cloud microservices backend, WeChat mini-program frontend, Vue3 admin panel. JDK 17, Spring Boot 3.2, Nacos, MySQL 8, Maven multi-module.

## Directory Layout

```
backend/                        # Maven multi-module parent pom
  common-core/                  # Shared utilities & dependencies
  api-gateway/                  # Spring Cloud Gateway (port 9090)
  auth-service/                 # WeChat login, token auth (port 9091)
  menu-service/                 # Menu CRUD (port 9093)
  order-service/                # Order management (port 9097)
  ai-chat-service/              # AI chat, RAG, intent recognition (port 9094)
  knowledge-service/            # RAG vector knowledge base (port 9095)
  admin-service/                # Admin panel backend (port 9096)
admin/                          # Vue3 admin panel (npm, Vite)
miniprogram/                    # WeChat mini-program
config/nacos/                   # Local Nacos installation & data
scripts/sql/                    # Database init scripts
scripts/nacos/                  # Nacos config publish script
scripts/cloud/                  # Multi-service startup scripts
```

## Build & Run

```bash
# Build a single service (from backend/)
mvn package -pl ai-chat-service -am -DskipTests -q

# Build everything
mvn clean package -DskipTests

# Run a single service via Maven
mvn spring-boot:run -pl ai-chat-service -am

# Run tests
mvn test
mvn test -Dtest=TokenServiceImplTest -pl auth-service

# Admin panel
cd admin && npm install && npm run dev

# Start all services (requires .env and running Nacos)
powershell -File scripts/cloud/start-services.ps1
```

**Important**: The admin `package.json` is in `admin/`, not root. `npm run dev` must be run from `admin/`.

## Nacos Configuration

Nacos runs locally at `localhost:8848` (credentials: nacos/nacos). Config is stored at `config/nacos/`.

### Publishing config via API

When using `curl` to POST config, `&` in YAML content breaks query-string parsing. **Always URL-encode the content**:

```powershell
$encoded = [System.Web.HttpUtility]::UrlEncode($config)
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "tenant=public&dataId=ai-chat-service.yaml&group=AIWECHAT_GROUP&content=$encoded"
```

### Config file location (runtime)

Nacos persists fetched configs to `config/nacos/data/tenant-config-data/<namespace>/<group>/<dataId>`. These files are in `.gitignore` — do not commit them.

### token.secret.key

Must be a Base64-encoded string of at least 32 raw bytes. The current key at `config/nacos/conf/application.properties:215` is valid.

## AI Chat Service Critical Facts

### MiMo (Xiaomi) API behavior

- AI 对话供应商已从 ModelScope 切换为小米 MiMo。单模型 `mimo-v2.5`（多模态，支持图片输入），不再多模型轮换。
- **必须发送 `thinking: {type: disabled}`**：MiMo 默认开启推理（生成 `reasoning_content`，慢且费 token）。所有 5 个对话调用点（`ModelScopeAICustomerServiceImpl` 主对话+图片路径、`IntentRecognitionServiceImpl`、`SessionMemoryServiceImpl`、`UserPreferenceServiceImpl`）的 requestBody 已加此参数，客服场景禁用推理以加速响应。
- API endpoint: `https://api.xiaomimimo.com/v1/chat/completions`（OpenAI 兼容）
- Key 在 Nacos `spring.ai.openai.api-key`（映射自 env `MODELSCOPE_API_KEY`，变量名沿用历史命名，实际存 MiMo key）
- 响应解析沿用标准 OpenAI `choices[0].message.content`，已有 `choices != null` 防护。

### 单模型 + 重试退避（无多模型轮换）

- `spring.ai.openai.models`（env `AI_MODELS`）只配 `mimo-v2.5`，`callModelScopeApi` 的 `attempt % models.size()` 恒为 0，不再轮换。
- **重试退避**：`maxAttempts = Math.max(models.size(), chatMaxAttempts)`，`chat.max-attempts` 默认 3。单模型时重试同一模型 3 次，非首次请求前 `Thread.sleep(800*attempt)` 退避，缓解限流/瞬时抖动。
- 历史背景：之前 ModelScope 多模型轮换 + `enable_thinking:false` 与 DeepSeek 不兼容导致 `choices: null`，已切 MiMo 单模型规避。

### Architecture notes

- `RestClient` is a shared Bean from `RestClientConfig` (connect=10s, read=60s), injected via Lombok `@RequiredArgsConstructor` in every service
- `@EnableCaching` is on `AiChatServiceApplication`; `getChatHistory` is no longer `@Cacheable` (removed so a Redis outage doesn't 500).
- `ChatRecordService` is a separate Bean (avoids self-invocation proxy issues)
- Intent recognition + RAG run in parallel via `CompletableFuture` (15s/10s timeouts)
- Session memory has TTL cleanup (every 10min, 2h idle) + disk persistence (every 5min snapshots)
- `ContentSafetyFilter` blocks prompt injection patterns
- Human transfer (`SimpleHumanTransferServiceImpl.shouldTransfer()`) is currently disabled (always returns `false`)

### Preference extraction

Currently disabled — both call sites in `ModelScopeAICustomerServiceImpl` are commented out with TODO markers. Do not enable until storage is implemented.

## Your Tasks: What We're Fixing

(已解决) 之前 ModelScope `DeepSeek-V4-Flash` 返回 `choices: null` 的问题，已通过切换到 MiMo 单模型 `mimo-v2.5` + `thinking:disabled` 规避。当前无遗留待修问题。

## References

- `CLAUDE.md` — detailed API endpoints, DB schema, frontend architecture
- `DIRECTORY.md` — directory descriptions
- `HOTRELOAD.md` — Spring DevTools configuration
- `README.md` — project overview & quick start
- `docs/` — additional project documentation
