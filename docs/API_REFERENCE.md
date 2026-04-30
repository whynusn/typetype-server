# TypeType API Reference

Self-contained API contract for the TypeType backend. Each endpoint section includes complete request/response JSON shapes — no external references required.

**Base URL**: `http://localhost:8080/api/v1`
**Auth**: JWT Bearer token in `Authorization` header (except auth endpoints)
**Content-Type**: `application/json` for all request/response bodies

---

## Common Envelope

Every response is wrapped in:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": <T>,
  "timestamp": 1704067200000
}
```

Paginated responses use `data` as:

```json
{
  "records": [...],
  "total": 100,
  "page": 1,
  "size": 20,
  "pages": 5
}
```

### Error Codes

| Code | Module | Meaning |
|------|--------|---------|
| 200 | — | Success |
| 10001 | System | Internal error |
| 10002 | System | Parameter validation failed |
| 10003 | System | Resource not found |
| 20001 | Auth | Token expired |
| 20002 | Auth | Token invalid |
| 20003 | Auth | Password error |
| 20004 | Auth | Username not found |
| 20005 | Auth | Username already exists |
| 20006 | Auth | Login failed (wrong credentials) |
| 30001 | Text | Text source not found |
| 30002 | Text | No available text |
| 40001 | Score | Score data invalid |
| 40002 | Score | Submit too frequent (5s cooldown) |

---

## Auth Module

### POST /auth/register

Register a new user.

**Request:**
```json
{
  "username": "john_doe",
  "password": "secret123",
  "confirmPassword": "secret123",
  "nickname": "John"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| username | String | yes | 3-20 chars, `[a-zA-Z0-9_]` |
| password | String | yes | 6-30 chars |
| confirmPassword | String | yes | Must match password |
| nickname | String | no | max 64 chars |

**Response** — `Result<UserVO>`:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "john_doe",
    "nickname": "John",
    "avatarUrl": null,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  },
  "timestamp": 1704067200000
}
```

---

### POST /auth/login

**Request:**
```json
{
  "username": "john_doe",
  "password": "secret123"
}
```

**Response** — `Result<TokenVO>`:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "username": "john_doe",
      "nickname": "John",
      "avatarUrl": null,
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  },
  "timestamp": 1704067200000
}
```

---

### POST /auth/refresh

Rotate tokens. Pass the **refresh token** in the `Authorization` header.

**Headers:**
```
Authorization: Bearer <refreshToken>
```

**Response** — `Result<TokenVO>` (same shape as login response).

---

### POST /auth/logout

**Headers:**
```
Authorization: Bearer <token>  (optional)
```

**Response:**
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null,
  "timestamp": 1704067200000
}
```

---

## Text Module

### GET /texts/catalog

Returns all active text sources.

**Response** — `Result<List<TextSource>>`:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "sourceKey": "cet4",
      "label": "CET-4 词汇",
      "category": "vocabulary",
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00"
    },
    {
      "id": 2,
      "sourceKey": "jisubei",
      "label": "极速杯",
      "category": "article",
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00"
    }
  ],
  "timestamp": 1704067200000
}
```

---

### GET /texts/source/{sourceKey}

Get a random text from a source. Uses offset-based random selection (no `ORDER BY RAND()`).

**Path:** `sourceKey` — one of `cet4`, `cet6`, `essay_classic`, `code_snippet`, `jisubei`, `custom`

**Response** — `Result<Text>`:
```json
{
  "code": 200,
  "data": {
    "id": 42,
    "sourceId": 1,
    "title": "abandon",
    "content": "to give up completely...",
    "charCount": 23,
    "difficulty": 0,
    "clientTextId": 123456789,
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

---

### GET /texts/latest/{sourceKey}

Get the most recent text from a source.

**Response** — `Result<Text>` (same shape as above).

---

### GET /texts/{id}

Get text by server-side ID.

**Path:** `id` — text primary key

**Response** — `Result<Text>` (same shape as above).

---

### GET /texts/by-source/{sourceKey}

Get all texts from a specific source.

**Response** — `Result<List<Text>>` (list of Text objects).

---

### GET /texts/by-client-text-id/{clientTextId}

Lookup text by client-computed hash ID. `clientTextId` is a decimal integer derived from `SHA-256(sourceKey:content)` → first 8 hex chars → decimal mod 10^9.

**Path:** `clientTextId` — decimal integer

**Response** — `Result<Text>` (same shape as above).

---

### POST /texts/upload (ADMIN only)

Upload a new text. Requires `ROLE_ADMIN`.

**Request:**
```json
{
  "title": "My Custom Text",
  "content": "The quick brown fox jumps over the lazy dog.",
  "sourceKey": "custom"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| title | String | no | Defaults to first 20 chars of content |
| content | String | yes | Text to type |
| sourceKey | String | yes | Must exist in t_text_source. Auto-creates if `custom` |

**Response** — `Result<Text>` (created text with server-assigned `clientTextId`).

---

## Score Module

### POST /scores

Submit a typing test score. **V2 contract**: client sends only raw fields; all derived metrics are computed server-side.

**Request:**
```json
{
  "textId": 42,
  "charCount": 300,
  "wrongCharCount": 5,
  "backspaceCount": 10,
  "correctionCount": 3,
  "keyStrokeCount": 750,
  "time": 120.0
}
```

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| textId | Long | no | — | Server text ID. null if offline text |
| charCount | Integer | yes | >= 0 | Total characters in the text |
| wrongCharCount | Integer | yes | >= 0 | Characters typed incorrectly |
| backspaceCount | Integer | yes | >= 0 | Backspace key presses |
| correctionCount | Integer | yes | >= 0 | Characters corrected via backspace-then-retype |
| keyStrokeCount | Integer | yes | >= 0 | Total key presses (including backspace, corrections) |
| time | BigDecimal | yes | >= 0 | Duration in seconds |

**Rate limit**: 5-second minimum interval between submissions per user.

**Server-side derived metrics** (computed on the Score entity, not stored in DB):
- `speed` = `charCount * 60 / time`
- `keyStroke` = `keyStrokeCount / time`
- `codeLength` = `keyStrokeCount / charCount`
- `keyAccuracy` = `(keyStrokeCount - wrongKeys) / keyStrokeCount * 100` where `wrongKeys = backspaceCount + correctionCount * codeLength`
- `effectiveSpeed` = `(charCount - wrongCharCount) * 60 / time`

**Response:**
```json
{
  "code": 200,
  "message": "成绩提交成功",
  "data": null,
  "timestamp": 1704067200000
}
```

---

### GET /scores/history

Current user's score history, ordered by `created_at DESC`.

**Query params**: `page` (default 1), `size` (default 20)

**Response** — `Result<PageResult<ScoreVO>>`:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 100,
        "textId": 42,
        "textTitle": "abandon",
        "speed": 150.00,
        "keyStroke": 6.25,
        "codeLength": 2.500,
        "charCount": 300,
        "wrongCharCount": 5,
        "keyAccuracy": 98.67,
        "backspaceCount": 10,
        "correctionCount": 3,
        "time": 120.0,
        "effectiveSpeed": 147.50,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20,
    "pages": 1
  }
}
```

Note: `speed`, `keyStroke`, `codeLength`, `keyAccuracy`, `effectiveSpeed` are **derived** — computed by `Score` entity getter methods from the stored raw fields. `charCount`, `wrongCharCount`, `backspaceCount`, `correctionCount`, `keyStrokeCount` (not in VO but stored), `time` are **raw** fields.

---

### GET /texts/{textId}/scores

Current user's scores for a specific text.

**Path:** `textId` — text primary key
**Query params**: `page` (default 1), `size` (default 20)

**Response** — same shape as `/scores/history`.

---

### GET /texts/{textId}/leaderboard

Leaderboard for a specific text. One entry per user (highest speed). Ranking by speed descending.

**Path:** `textId` — text primary key
**Query params**: `page` (default 1), `size` (default 50)

**Response** — `Result<PageResult<LeaderboardVO>>`:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "rank": 1,
        "userId": 1,
        "username": "speedtyper",
        "nickname": "Speed Demon",
        "avatarUrl": null,
        "speed": 180.50,
        "keyStroke": 7.20,
        "codeLength": 2.400,
        "charCount": 300,
        "wrongCharCount": 2,
        "keyAccuracy": 99.20,
        "backspaceCount": 5,
        "correctionCount": 1,
        "time": 100.0,
        "effectiveSpeed": 178.80,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "total": 15,
    "page": 1,
    "size": 50,
    "pages": 1
  }
}
```

---

### GET /texts/{textId}/best

Current user's best score for a specific text. Returns `null` if no scores exist.

**Path:** `textId` — text primary key

**Response** — `Result<ScoreVO>` or `Result<null>`:
```json
{
  "code": 200,
  "data": {
    "id": 100,
    "textId": 42,
    "textTitle": "abandon",
    "speed": 150.00,
    "keyStroke": 6.25,
    "codeLength": 2.500,
    "charCount": 300,
    "wrongCharCount": 5,
    "keyAccuracy": 98.67,
    "backspaceCount": 10,
    "correctionCount": 3,
    "time": 120.0,
    "effectiveSpeed": 147.50,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

---

## User Module

### GET /users/me

Returns the currently authenticated user.

**Response** — `Result<UserVO>`:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "john_doe",
    "nickname": "John",
    "avatarUrl": null,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

---

### GET /users/{id} (ADMIN only)

Returns a user by ID. Requires `ROLE_ADMIN`.

**Path:** `id` — user primary key

**Response** — `Result<UserVO>` (same shape as above).

---

## Security

### Authentication Flow

1. Client sends `POST /auth/login` → receives `accessToken` + `refreshToken`
2. All subsequent requests include `Authorization: Bearer <accessToken>`
3. `JwtAuthenticationFilter` validates token on every request
4. When access token expires (401), client calls `POST /auth/refresh` with refresh token
5. Server rotates both tokens (old refresh token is invalidated)

### JWT Configuration

| Parameter | Value |
|-----------|-------|
| Algorithm | HMAC-SHA256 |
| Secret | `JWT_SECRET_KEY` env var |
| Access token TTL | 15 minutes |
| Refresh token TTL | 7 days |
| Issuer | typetype-server |

### Public Endpoints (no auth required)

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### Admin-Only Endpoints

- `POST /texts/upload`
- `GET /users/{id}`

### Rate Limiting

Applied to `/api/v1/auth/**` via Bucket4j (per-IP):
- Login: 10 requests/min
- Register: 5 requests/min
- Other auth: 20 requests/min

Score submission has application-level rate limiting: 5-second minimum interval per user.
