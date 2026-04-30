# TypeType 后端架构

后端架构的自包含参考。每个模块章节可独立阅读。

**技术栈**: Spring Boot 3.2.5 / Java 21 / MyBatis 3.0.3 / MySQL / Flyway / JWT (jjwt 0.12.6)

---

## 包结构

```
com.typetype
├── common/           共享基础设施
│   ├── config/       SecurityConfig（Spring Security 过滤链）
│   ├── exception/    GlobalExceptionHandler、BusinessException
│   ├── filter/       RateLimitFilter（Bucket4j 限流）
│   ├── result/       Result<T>、PageResult<T>、ResultCode
│   └── util/         SecurityUtils
├── auth/             认证模块（JWT）
│   ├── config/       JwtProperties、JwtAuthenticationEntryPoint
│   ├── controller/   AuthController
│   ├── dto/          RegisterDTO、LoginDTO、TokenVO、JwtPayloadDTO
│   ├── filter/       JwtAuthenticationFilter
│   └── service/      AuthService、JwtService
├── user/             用户模块
│   ├── constant/     UserRole
│   ├── controller/   UserController
│   ├── converter/    UserConverter
│   ├── dto/          UserVO
│   ├── entity/       User
│   ├── mapper/       UserMapper
│   └── service/      UserService
├── text/             文本模块
│   ├── controller/   TextController
│   ├── dto/          UploadTextDTO、FetchedTextDTO
│   ├── entity/       Text、TextSource
│   ├── mapper/       TextMapper、TextSourceMapper
│   ├── service/      TextService、TextFetchService、SaiWenTextFetcher
│   └── task/         DailyJisubeiFetchTask
└── score/            成绩模块
    ├── controller/   ScoreController
    ├── dto/          SubmitScoreDTO、ScoreVO、LeaderboardVO
    ├── entity/       Score
    ├── mapper/       ScoreMapper
    └── service/      ScoreService
```

---

## 模块: auth（认证）

**职责**: 用户注册、登录、JWT 令牌生命周期。

**核心类**:

- `JwtService` — 使用 jjwt 0.12.6 生成/校验 JWT。复用 `JwtParser` 实例提升性能。Token 载荷包含：`userId`、`username`、`role`、`tokenType`（access/refresh）、`iat`、`exp`、`iss`、`sub`。

- `JwtAuthenticationFilter` — `OncePerRequestFilter`。从 `Authorization` 头提取 Bearer token，校验后构建 `UsernamePasswordAuthenticationToken`（角色前缀 `ROLE_`），设置到 `SecurityContext`。

- `AuthService` — 业务逻辑：注册（BCrypt + 重复检查）、登录（凭证校验 + token 对生成）、刷新（令牌轮换）、登出（撤销桩，预留给 Redis）。

**认证流程**: 客户端 → 登录 → 获得 accessToken + refreshToken → 请求携带 accessToken → 收到 401 时用 refreshToken 换取新 token 对（轮换）。

---

## 模块: user（用户）

**职责**: 用户资料 CRUD。

**核心类**:

- `UserMapper` — t_user 的 MyBatis mapper。方法：`findById`、`findByUsername`、`insert`。

- `UserService` — 凭证校验，委托 mapper 操作。

- `UserController` — `GET /users/me`（从 SecurityContext 获取当前用户）、`GET /users/{id}`（仅 ADMIN）。

**角色**: `USER`（默认）、`ADMIN`。定义在 `UserRole` 常量中。

---

## 模块: text（文本）

**职责**: 文本 CRUD、文本来源目录、外部 API 抓取、定时任务。

**核心类**:

- `TextService` — 核心业务：目录列表、随机文本（offset 策略，避免 `ORDER BY RAND()`）、最新文本、上传时服务端计算 `clientTextId`。

- `TextFetchService` — 从赛文 API 抓取文本。通过 `SHA-256(sourceKey:content)` → 前 8 位十六进制 → 十进制 mod 10^9 计算 `clientTextId`。按标题去重。

- `SaiWenTextFetcher` — 赛文 API HTTP 客户端（`https://api.saiwenshu.com/api/article`）。

- `DailyJisubeiFetchTask` — `@Scheduled(fixedDelay=10min)`。每天 06:00–23:59 自动抓取极速杯文章。使用 `volatile` 标志位做并发防护和日期去重（单实例无需分布式锁）。

- `TextController` — 7 个端点：目录、随机、最新、按 ID、按来源、按 clientTextId、上传（ADMIN）。

**外部依赖**: 赛文 API 用于文章抓取。失败记录日志，下一周期重试。

---

## 模块: score（成绩）

**职责**: 成绩提交、排行榜、历史记录、最佳成绩查询。

**核心类**:

- `Score` — V2 指标集实体。**数据库仅存储原始字段**（charCount、wrongCharCount、backspaceCount、correctionCount、keyStrokeCount、time）。所有派生指标（speed、keyStroke、codeLength、keyAccuracy、effectiveSpeed）通过实体 getter 方法计算 — 是指标计算的唯一数据源。

- `ScoreService` — 提交成绩（5 秒冷却）、排行榜（每用户一条最高速度记录，按速度降序）、用户历史、最佳成绩。通过 `toScoreVO()` 调用实体 getter 将 Score → ScoreVO。

- `ScoreMapper` — 内联 SQL 的 MyBatis mapper。关键查询：`insert`（仅 V2 原始字段）、`findLeaderboardByTextId`（子查询 + 排名）、`findBestScore`、`findByUserId`。

- `ScoreController` — 5 个端点：提交、用户历史、用户文本历史、排行榜、最佳成绩。

**成绩提交数据流**:
```
客户端发送原始字段 → SubmitScoreDTO → ScoreService 校验
→ Score.builder() 构建实体 → ScoreMapper.insert() 存储原始字段
→ 派生指标不落库
```

**排行榜查询数据流**:
```
ScoreMapper.findLeaderboardByTextId() 在 SQL 中通过 ROUND 表达式
计算派生指标 → 映射到 LeaderboardVO
```

---

## 模块: common（公共）

**职责**: 所有模块共享的基础设施。

- `SecurityConfig` — Spring Security 过滤链。禁用 CSRF，启用 CORS。JWT 过滤器在 `UsernamePasswordAuthenticationFilter` 之前。BCrypt 密码编码器。公开端点：`/api/v1/auth/**`。

- `RateLimitFilter` — Bucket4j 按 IP 限流（认证端点）。登录 10 次/分，注册 5 次/分，其他 20 次/分。

- `GlobalExceptionHandler` — `@RestControllerAdvice`。捕获 `BusinessException`、参数校验错误（`MethodArgumentNotValidException`）、通用 `RuntimeException`，返回 `Result<T>` 格式的错误响应。

- `Result<T>` — 统一响应信封：`{code, message, data, timestamp}`。

- `PageResult<T>` — 分页响应：`{records, total, page, size, pages}`。

- `SecurityUtils` — 从 `SecurityContext` 提取 `userId` 的静态工具类。

---

## 配置

**环境**: `default`（开发）、`dev`、`prod`

| 配置项 | 值 | 来源 |
|--------|-----|------|
| 服务端口 | 8080 | application.yml |
| MySQL | localhost:3306/typetype | application.yml |
| JWT 密钥 | 环境变量 `JWT_SECRET_KEY` | application.yml |
| JWT access TTL | 15 分钟 | application.yml |
| JWT refresh TTL | 7 天 | application.yml |
| Flyway | 启用，启动时自动迁移 | application.yml |
| Redis | 已注释（MVP 阶段） | pom.xml |

---

## 外部集成

| 服务 | 用途 | 失败处理 |
|------|------|----------|
| 赛文 API | 每日抓取极速杯文章 | 记录日志，下一周期重试 |
| MySQL | 持久化存储 | 应用启动失败 |
| Redis | （计划中）令牌撤销、缓存 | 尚未集成 |
