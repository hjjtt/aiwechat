# AGENTS.md

## Project Overview

WeChat Mini-program AI customer service system: Spring Cloud microservices backend, WeChat mini-program frontend, Vue3 admin panel. JDK 17, Spring Boot 3.2, Nacos, MySQL 8, Maven multi-module.

## Directory Layout

```
backend/                        # Maven multi-module parent pom
  common-core/                  # Shared utilities & dependencies
  api-gateway/                  # Spring Cloud Gateway (port 9000)
  auth-service/                 # WeChat login, token auth (port 9091)
  menu-service/                 # Menu CRUD (port 9092)
  order-service/                # Order management
  ai-chat-service/              # AI chat, RAG, intent recognition (port 9094)
  knowledge-service/            # RAG vector knowledge base (port 9095)
  admin-service/                # Admin panel backend
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

### ModelScope API behavior

- **Never send `enable_thinking: false`** — causes `deepseek-ai/DeepSeek-V4-Flash` to return HTTP 200 with `"choices": null`. All 4 callers (`ModelScopeAICustomerServiceImpl`, `IntentRecognitionServiceImpl`, `SessionMemoryServiceImpl`, `UserPreferenceServiceImpl`) must omit this parameter.
- Some models fail with 400 "Invalid model id" (e.g., `Qwen/Qwen3-35B-A3B`). Verify models work before adding to rotation.
- API endpoint: `https://api-inference.modelscope.cn/v1/chat/completions`
- Key is in Nacos `spring.ai.openai.api-key`

### Model rotation

Models configured via `spring.ai.openai.models` in Nacos (comma-separated list). The `currentModelIndex` rotates across calls; within a call, all models are tried sequentially. **Add a `choices != null` guard** to all response parsers — the API may return `"choices": null` instead of missing key.

### Architecture notes

- `RestClient` is a shared Bean from `RestClientConfig` (connect=10s, read=60s), injected via Lombok `@RequiredArgsConstructor` in every service
- `@EnableCaching` is on `AiChatServiceApplication`; `@Cacheable` on `getChatHistory`
- `ChatRecordService` is a separate Bean (avoids self-invocation proxy issues)
- Intent recognition + RAG run in parallel via `CompletableFuture` (15s/10s timeouts)
- Session memory has TTL cleanup (every 10min, 2h idle) + disk persistence (every 5min snapshots)
- `ContentSafetyFilter` blocks prompt injection patterns
- Human transfer (`SimpleHumanTransferServiceImpl.shouldTransfer()`) is currently disabled (always returns `false`)

### Preference extraction

Currently disabled — both call sites in `ModelScopeAICustomerServiceImpl` are commented out with TODO markers. Do not enable until storage is implemented.

## Your Tasks: What We're Fixing

The `deepseek-ai/DeepSeek-V4-Flash` model currently returns empty responses (`"choices": null`) for many prompts. `deepseek-ai/DeepSeek-V3.2` works as a fallback. The root cause is still under investigation — add response body logging to diagnose.

## References

- `CLAUDE.md` — detailed API endpoints, DB schema, frontend architecture
- `DIRECTORY.md` — directory descriptions
- `HOTRELOAD.md` — Spring DevTools configuration
- `README.md` — project overview & quick start
- `docs/` — additional project documentation
