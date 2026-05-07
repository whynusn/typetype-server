# TypeType Server

[Typetype](https://github.com/whynusn/typetype) 打字练习平台的后端服务。提供用户认证、文本管理、成绩提交与排行榜等 RESTful API。

**技术栈**: Spring Boot 3.2.5 / Java 21 / MyBatis / MySQL / Redis / Flyway / JWT / Docker

---

## 核心特性

- **JWT 双 Token 认证** — Access Token (15min) + Refresh Token (7d)，支持 Token Rotation 和 Redis 服务端撤销
- **Redis 多级缓存** — Sorted Set 实时排行榜、`@Cacheable` 声明式文本缓存、Refresh Token 有状态存储
- **分布式限流** — Redis INCR+EXPIRE 实现全局限流，支持降级策略（Redis 不可用时放行）
- **SSE 实时推送** — 排行榜变更时主动推送给客户端，基于 `SseEmitter` 连接池
- **定时爬取** — 每日自动抓取极速杯文章（SaiWen API），AES-128-CBC 加密通信
- **Docker 一键部署** — 多阶段构建、docker-compose 编排 MySQL + Redis + App
- **CI/CD** — GitHub Actions 自动构建、测试、打包、Docker 镜像构建
- **可观测性** — 结构化日志（logback）、请求链路追踪（MDC traceId）、Actuator 健康检查

## 快速启动

### Docker Compose（推荐）

```bash
# 1. 创建 .env（首次）
cat > .env <<EOF
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=typetype
REDIS_PASSWORD=your-redis-password
JWT_SECRET_KEY=$(openssl rand -hex 32)
APP_PORT=8080
EOF

# 2. 一键启动
docker compose up -d

# 3. 查看日志
docker compose logs -f app
```

服务启动后访问：
- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 健康检查: `http://localhost:8080/actuator/health`

### 本地开发

```bash
# 前提：本地已运行 MySQL 8.0 和 Redis
mvn spring-boot:run
```

## 项目结构

```
com.typetype
├── common/           共享基础设施
│   ├── config/       SecurityConfig（Spring Security 过滤链）
│   ├── exception/    GlobalExceptionHandler、BusinessException
│   ├── filter/       RateLimitFilter（Redis 分布式限流）、MdcFilter（链路追踪）
│   └── result/       Result<T>、PageResult<T>、ResultCode（统一响应）
├── auth/             认证模块
│   ├── service/      AuthService（登录/注册/刷新/登出）、JwtService（JWT 操作）
│   └── filter/       JwtAuthenticationFilter（Bearer Token 校验）
├── user/             用户模块
├── text/             文本模块
│   ├── service/      TextService（CRUD + @Cacheable）、TextFetchService、SaiWenTextFetcher
│   └── task/         DailyJisubeiFetchTask（定时爬取）
└── score/            成绩模块
    ├── service/      ScoreService（提交/排行榜/Redis 缓存/SSE 推送）
    └── controller/   ScoreController、LeaderboardSseController
```

采用 **feature-first** 包结构：每个模块按职责细分为 controller / service / mapper / entity / dto。

## 核心设计

### 认证流程

```
客户端                    服务端
  │                        │
  ├─ POST /auth/login ────>│ 验证凭证 → 生成 accessToken + refreshToken
  │<── Token 对 ───────────┤ refreshToken 存入 Redis（TTL 7d）
  │                        │
  ├─ GET /texts/catalog ──>│ JwtAuthenticationFilter 校验 accessToken
  │<── 数据 ───────────────┤
  │                        │
  ├─ POST /auth/refresh ──>│ 校验 refreshToken → 比对 Redis
  │<── 新 Token 对 ────────┤ 生成新 Token 对 + 更新 Redis（Token Rotation）
  │                        │
  ├─ POST /auth/logout ───>│ 从 Redis 删除 refreshToken（服务端撤销）
  │<── OK ─────────────────┤
```

### Redis 缓存策略

| 用途 | 数据结构 | Key 格式 | 说明 |
|------|----------|----------|------|
| 排行榜 | Sorted Set | `leaderboard:text:{textId}` | ZADD 按 speed 排序，每个用户保留最高速度 |
| 文本缓存 | String | `textById::{id}` | @Cacheable 声明式，写入时 @CacheEvict 驱逐 |
| 目录缓存 | String | `textCatalog::activeSources` | 文本来源列表，上传新文本时驱逐 |
| Refresh Token | String | `refresh_token:{userId}` | TTL 7 天，支持服务端撤销 |
| 限流计数 | String | `ratelimit:{ip}` | INCR + EXPIRE，1 分钟窗口 |

### 分布式限流

```
请求 → RateLimitFilter
        │
        ├─ Redis INCR "ratelimit:{ip}"
        │   ├─ 返回 1 → 设置 EXPIRE 60s
        │   ├─ 返回 > limit → 拒绝 (429)
        │   └─ Redis 异常 → 降级放行
        │
        └─ 放行到下一个 Filter
```

限流规则：登录 10次/分、注册 5次/分、其他认证接口 20次/分。

## API 概览

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/auth/register` | 注册 | - |
| POST | `/api/v1/auth/login` | 登录 | - |
| POST | `/api/v1/auth/refresh` | 刷新 Token | - |
| POST | `/api/v1/auth/logout` | 登出 | Bearer |
| GET | `/api/v1/users/me` | 当前用户信息 | Bearer |
| GET | `/api/v1/texts/catalog` | 文本来源目录 | Bearer |
| GET | `/api/v1/texts/latest/{sourceKey}` | 最新文本 | Bearer |
| GET | `/api/v1/texts/{id}` | 文本详情 | Bearer |
| POST | `/api/v1/texts/upload` | 上传文本 | ADMIN |
| POST | `/api/v1/scores` | 提交成绩 | Bearer |
| GET | `/api/v1/texts/{textId}/leaderboard` | 排行榜 | Bearer |
| GET | `/api/v1/texts/{textId}/leaderboard/stream` | 排行榜 SSE | Bearer |

完整 API 文档见 [`docs/API_REFERENCE.md`](docs/API_REFERENCE.md)。

## 数据库

使用 Flyway 自动迁移，启动时按版本号顺序执行：

| 迁移 | 说明 |
|------|------|
| V1 | 创建用户、文本来源、文本、成绩四张表 + 示例数据 |
| V2 | 用户表添加 role 字段 |
| V3 | 添加极速杯文本来源 |
| V4 | 排行榜查询优化索引 |
| V5 | 添加自定义文本来源 |
| V6 | 文本表添加 clientTextId 字段 |
| V9 | 成绩表字段重命名迁移 |

详细 schema 见 [`docs/DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md)。

## 部署

详细部署指南见 [`docs/DEPLOYMENT.md`](docs/DATABASE_SCHEMA.md)。

```bash
# 开发机构建镜像
docker build -t typetype-server:v1.0 .

# 推送到镜像仓库
docker tag typetype-server:v1.0 registry.cn-hangzhou.aliyuncs.com/your-ns/typetype-server:v1.0
docker push registry.cn-hangzhou.aliyuncs.com/your-ns/typetype-server:v1.0

# 生产服务器拉取并启动
docker pull registry.cn-hangzhou.aliyuncs.com/your-ns/typetype-server:v1.0
docker compose up -d
```

## 文档索引

- [`docs/API_REFERENCE.md`](docs/API_REFERENCE.md) — 接口契约（请求/响应 JSON）
- [`docs/DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md) — 数据库 schema 与迁移历史
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — 模块结构与数据流
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — 部署指南
- [`docs/INTERVIEW_GUIDE.md`](docs/INTERVIEW_GUIDE.md) — 面试重点与技术原理
