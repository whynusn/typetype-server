# TypeType Server Architecture

Self-contained reference for the backend architecture. Each module section is independently readable.

**Stack**: Spring Boot 3.2.5 / Java 21 / MyBatis 3.0.3 / MySQL / Flyway / JWT (jjwt 0.12.6)

---

## Package Structure

```
com.typetype
├── common/           Shared infrastructure
│   ├── config/       SecurityConfig (Spring Security filter chain)
│   ├── exception/    GlobalExceptionHandler, BusinessException
│   ├── filter/       RateLimitFilter (Bucket4j)
│   ├── result/       Result<T>, PageResult<T>, ResultCode
│   └── util/         SecurityUtils
├── auth/             Authentication (JWT)
│   ├── config/       JwtProperties, JwtAuthenticationEntryPoint
│   ├── controller/   AuthController
│   ├── dto/          RegisterDTO, LoginDTO, TokenVO, JwtPayloadDTO
│   ├── filter/       JwtAuthenticationFilter
│   └── service/      AuthService, JwtService
├── user/             User management
│   ├── constant/     UserRole
│   ├── controller/   UserController
│   ├── converter/    UserConverter
│   ├── dto/          UserVO
│   ├── entity/       User
│   ├── mapper/       UserMapper
│   └── service/      UserService
├── text/             Typing text management
│   ├── controller/   TextController
│   ├── dto/          UploadTextDTO, FetchedTextDTO
│   ├── entity/       Text, TextSource
│   ├── mapper/       TextMapper, TextSourceMapper
│   ├── service/      TextService, TextFetchService, SaiWenTextFetcher
│   └── task/         DailyJisubeiFetchTask
└── score/            Score/leaderboard management
    ├── controller/   ScoreController
    ├── dto/          SubmitScoreDTO, ScoreVO, LeaderboardVO
    ├── entity/       Score
    ├── mapper/       ScoreMapper
    └── service/      ScoreService
```

---

## Module: auth

**Responsibility**: User registration, login, JWT token lifecycle.

**Key classes**:

- `JwtService` — Generates/verifies JWT tokens using jjwt 0.12.6. Reuses `JwtParser` instance for performance. Tokens contain: `userId`, `username`, `role`, `tokenType` (access/refresh), `iat`, `exp`, `iss`, `sub`.

- `JwtAuthenticationFilter` — `OncePerRequestFilter`. Extracts Bearer token from `Authorization` header, validates it, builds `UsernamePasswordAuthenticationToken` with `ROLE_`-prefixed authorities, sets it in `SecurityContext`.

- `AuthService` — Business logic: register (BCrypt + duplicate check), login (credential validation + token pair), refresh (token rotation), logout (revocation stubs for Redis).

**Auth flow**: Client → login → gets accessToken + refreshToken → uses accessToken in header → on 401, uses refreshToken to get new pair (rotation).

---

## Module: user

**Responsibility**: User profile CRUD.

**Key classes**:

- `UserMapper` — MyBatis mapper for t_user. Methods: `findById`, `findByUsername`, `insert`.

- `UserService` — Validates credentials, delegates to mapper.

- `UserController` — `GET /users/me` (current user from SecurityContext), `GET /users/{id}` (ADMIN only).

**Roles**: `USER` (default), `ADMIN`. Defined in `UserRole` constants.

---

## Module: text

**Responsibility**: Text CRUD, text source catalog, external API fetching, scheduled tasks.

**Key classes**:

- `TextService` — Core business logic: catalog listing, random text (offset-based, avoids `ORDER BY RAND()`), latest text, upload with server-computed `clientTextId`.

- `TextFetchService` — Fetches texts from SaiWen external API. Computes `clientTextId` via `SHA-256(sourceKey:content)` → first 8 hex chars → decimal mod 10^9. Deduplicates by title.

- `SaiWenTextFetcher` — HTTP client for SaiWen API (`https://api.saiwenshu.com/api/article`).

- `DailyJisubeiFetchTask` — `@Scheduled(fixedDelay=10min)`. Fetches Jisubei articles between 06:00–23:59. Uses `volatile` flags for concurrency guard and date-based dedup (no distributed lock needed for single-instance).

- `TextController` — 7 endpoints: catalog, random, latest, by-id, by-source, by-client-text-id, upload (ADMIN).

**External dependency**: SaiWen API for article fetching. Failure is logged and retried on next cycle.

---

## Module: score

**Responsibility**: Score submission, leaderboard, history, best-score queries.

**Key classes**:

- `Score` — Entity with V2 metrics. **Only stores raw fields** in the database (charCount, wrongCharCount, backspaceCount, correctionCount, keyStrokeCount, time). All derived metrics (speed, keyStroke, codeLength, keyAccuracy, effectiveSpeed) are computed via getter methods on the entity — the single source of truth for metric calculations.

- `ScoreService` — Submit score (5-second cooldown), leaderboard (one entry per user, ranked by speed), user history, best score. Converts `Score` entity → `ScoreVO` via `toScoreVO()` which calls entity getters.

- `ScoreMapper` — MyBatis mapper with inline SQL. Key queries: `insert` (V2 raw fields only), `findLeaderboardByTextId` (subquery + ranking), `findBestScore`, `findByUserId`.

- `ScoreController` — 5 endpoints: submit, user history, user text history, leaderboard, best.

**Data flow for score submission**:
```
Client sends raw fields → SubmitScoreDTO → ScoreService validates
→ Score.builder() creates entity → ScoreMapper.insert() stores raw fields
→ Derived metrics never touch the database
```

**Data flow for leaderboard query**:
```
ScoreMapper.findLeaderboardByTextId() computes derived metrics
in SQL (ROUND expressions) → maps to LeaderboardVO
```

---

## Module: common

**Responsibility**: Shared infrastructure used by all modules.

- `SecurityConfig` — Spring Security filter chain. Disables CSRF, enables CORS. JWT filter before `UsernamePasswordAuthenticationFilter`. BCrypt password encoder. Public endpoints: `/api/v1/auth/**`.

- `RateLimitFilter` — Bucket4j per-IP rate limiting for auth endpoints. Login: 10/min, register: 5/min, other: 20/min.

- `GlobalExceptionHandler` — `@RestControllerAdvice`. Catches `BusinessException`, validation errors (`MethodArgumentNotValidException`), and generic `RuntimeException`. Returns `Result<T>` with appropriate error codes.

- `Result<T>` — Unified response envelope: `{code, message, data, timestamp}`.

- `PageResult<T>` — Paginated response: `{records, total, page, size, pages}`.

- `SecurityUtils` — Static helper to extract `userId` from `SecurityContext`.

---

## Configuration

**Profiles**: `default` (dev), `dev`, `prod`

| Setting | Value | Source |
|---------|-------|--------|
| Server port | 8080 | application.yml |
| MySQL | localhost:3306/typetype | application.yml |
| JWT secret | env `JWT_SECRET_KEY` | application.yml |
| JWT access TTL | 15 min | application.yml |
| JWT refresh TTL | 7 days | application.yml |
| Flyway | enabled, auto-migrate on startup | application.yml |
| Redis | commented out (MVP stage) | pom.xml |

---

## External Integrations

| Service | Purpose | Failure Mode |
|---------|---------|--------------|
| SaiWen API | Fetch daily Jisubei articles | Logged, retried next cycle |
| MySQL | Persistent storage | App fails to start |
| Redis | (Planned) Token revocation, caching | Not yet integrated |
