# TypeType 接口参考

TypeType 后端的自包含 API 契约。每个端点包含完整的请求/响应 JSON 结构，无需交叉引用其他文档。

**基础路径**: `http://localhost:8080/api/v1`
**认证方式**: JWT Bearer Token（认证端点除外）
**Content-Type**: `application/json`

---

## 通用响应格式

所有接口响应统一包装为：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": <T>,
  "timestamp": 1704067200000
}
```

分页响应中 `data` 结构为：

```json
{
  "records": [...],
  "total": 100,
  "page": 1,
  "size": 20,
  "pages": 5
}
```

### 错误码

| 错误码 | 模块 | 含义 |
|--------|------|------|
| 200 | — | 成功 |
| 10001 | 系统 | 内部异常 |
| 10002 | 系统 | 参数校验失败 |
| 10003 | 系统 | 资源不存在 |
| 20001 | 认证 | Token 已过期 |
| 20002 | 认证 | Token 无效 |
| 20003 | 认证 | 密码错误 |
| 20004 | 认证 | 用户名不存在 |
| 20005 | 认证 | 用户名已存在 |
| 20006 | 认证 | 登录失败（用户名或密码错误） |
| 30001 | 文本 | 文本来源不存在 |
| 30002 | 文本 | 无可用文本 |
| 40001 | 成绩 | 成绩数据异常 |
| 40002 | 成绩 | 提交过于频繁（5 秒冷却） |

---

## 认证模块

### POST /auth/register

注册新用户。

**请求体：**
```json
{
  "username": "john_doe",
  "password": "secret123",
  "confirmPassword": "secret123",
  "nickname": "John"
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| username | String | 是 | 3-20 字符，`[a-zA-Z0-9_]` |
| password | String | 是 | 6-30 字符 |
| confirmPassword | String | 是 | 必须与 password 一致 |
| nickname | String | 否 | 最多 64 字符 |

**响应** — `Result<UserVO>`：
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
  }
}
```

---

### POST /auth/login

**请求体：**
```json
{
  "username": "john_doe",
  "password": "secret123"
}
```

**响应** — `Result<TokenVO>`：
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
  }
}
```

---

### POST /auth/refresh

令牌轮换。在 `Authorization` 头中传入 **refresh token**。

**请求头：**
```
Authorization: Bearer <refreshToken>
```

**响应** — `Result<TokenVO>`（结构同登录响应）。

---

### POST /auth/logout

**请求头：**
```
Authorization: Bearer <token>  （可选）
```

**响应：**
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

---

## 文本模块

### GET /texts/catalog

获取所有已启用的文本来源。

**响应** — `Result<List<TextSource>>`：
```json
{
  "code": 200,
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
  ]
}
```

---

### GET /texts/source/{sourceKey}

获取指定来源的随机文本。使用 offset 随机策略（非 `ORDER BY RAND()`）。

**路径参数：** `sourceKey` — 可选值：`cet4`、`cet6`、`essay_classic`、`code_snippet`、`jisubei`、`custom`

**响应** — `Result<Text>`：
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

获取指定来源的最新文本。

**响应** — `Result<Text>`（结构同上）。

---

### GET /texts/{id}

通过服务端 ID 获取文本。

**响应** — `Result<Text>`（结构同上）。

---

### GET /texts/by-source/{sourceKey}

获取指定来源下的所有文本。

**响应** — `Result<List<Text>>`。

---

### GET /texts/by-client-text-id/{clientTextId}

通过客户端哈希 ID 查找文本。`clientTextId` 为十进制整数，由 `SHA-256(sourceKey:content)` 前 8 位十六进制 → 十进制 mod 10^9 计算得出。

**路径参数：** `clientTextId` — 十进制整数

**响应** — `Result<Text>`（结构同上）。

---

### POST /texts/upload（仅 ADMIN）

上传新文本。需要 `ROLE_ADMIN` 权限。

**请求体：**
```json
{
  "title": "我的自定义文本",
  "content": "The quick brown fox jumps over the lazy dog.",
  "sourceKey": "custom"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 否 | 默认取 content 前 20 字符 |
| content | String | 是 | 待打字的文本内容 |
| sourceKey | String | 是 | 必须已存在于 t_text_source。若为 `custom` 则自动创建 |

**响应** — `Result<Text>`（包含服务端分配的 `clientTextId`）。

---

## 成绩模块

### POST /scores

提交打字测试成绩。**V2 契约**：客户端仅发送原始字段，所有派生指标由服务端统一计算。

**请求体：**
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

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| textId | Long | 否 | — | 服务端文本 ID，离线文本可为 null |
| charCount | Integer | 是 | >= 0 | 文本总字符数 |
| wrongCharCount | Integer | 是 | >= 0 | 错误字符数 |
| backspaceCount | Integer | 是 | >= 0 | 退格键按下次数 |
| correctionCount | Integer | 是 | >= 0 | 回改字数（退格后重打） |
| keyStrokeCount | Integer | 是 | >= 0 | 总按键次数（含退格、回改） |
| time | BigDecimal | 是 | >= 0 | 用时（秒） |

**提交频率限制**：同一用户两次提交间隔不少于 5 秒。

**服务端派生指标**（由 Score 实体 getter 计算，不存入数据库）：
- `speed` = `charCount * 60 / time`（字/分）
- `keyStroke` = `keyStrokeCount / time`（击/秒）
- `codeLength` = `keyStrokeCount / charCount`（击/字）
- `keyAccuracy` = `(keyStrokeCount - wrongKeys) / keyStrokeCount * 100`（%），其中 `wrongKeys = backspaceCount + correctionCount * codeLength`
- `effectiveSpeed` = `(charCount - wrongCharCount) * 60 / time`（字/分）

**响应：**
```json
{
  "code": 200,
  "message": "成绩提交成功",
  "data": null
}
```

---

### GET /scores/history

当前用户的成绩历史，按 `created_at DESC` 排序。

**查询参数：** `page`（默认 1）、`size`（默认 20）

**响应** — `Result<PageResult<ScoreVO>>`：
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

> **注意：** `speed`、`keyStroke`、`codeLength`、`keyAccuracy`、`effectiveSpeed` 为**派生字段**，由 Score 实体 getter 从存储的原始字段计算而来。`charCount`、`wrongCharCount`、`backspaceCount`、`correctionCount`、`time` 为**原始字段**。

---

### GET /texts/{textId}/scores

当前用户在指定文本下的成绩历史。

**路径参数：** `textId` — 文本主键
**查询参数：** `page`（默认 1）、`size`（默认 20）

**响应** — 结构同 `/scores/history`。

---

### GET /texts/{textId}/leaderboard

指定文本的排行榜。每个用户仅保留最高速度的一条记录，按速度降序排列。

**路径参数：** `textId` — 文本主键
**查询参数：** `page`（默认 1）、`size`（默认 50）

**响应** — `Result<PageResult<LeaderboardVO>>`：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "rank": 1,
        "userId": 1,
        "username": "speedtyper",
        "nickname": "极速达人",
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

当前用户在指定文本下的最佳成绩。无成绩时返回 `null`。

**路径参数：** `textId` — 文本主键

**响应** — `Result<ScoreVO>` 或 `Result<null>`：
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

## 用户模块

### GET /users/me

获取当前已认证用户的信息。

**响应** — `Result<UserVO>`：
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

### GET /users/{id}（仅 ADMIN）

通过 ID 获取用户信息。需要 `ROLE_ADMIN` 权限。

**路径参数：** `id` — 用户主键

**响应** — `Result<UserVO>`（结构同上）。

---

## 安全配置

### 认证流程

1. 客户端调用 `POST /auth/login` → 获得 `accessToken` + `refreshToken`
2. 后续请求在 `Authorization` 头中携带 `Bearer <accessToken>`
3. `JwtAuthenticationFilter` 在每个请求上校验 token
4. Access token 过期（401）时，客户端使用 refresh token 调用 `POST /auth/refresh`
5. 服务端执行令牌轮换（旧 refresh token 失效，返回新 token 对）

### JWT 配置

| 参数 | 值 |
|------|-----|
| 算法 | HMAC-SHA256 |
| 密钥 | 环境变量 `JWT_SECRET_KEY` |
| Access token 有效期 | 15 分钟 |
| Refresh token 有效期 | 7 天 |
| 签发者 | typetype-server |

### 公开端点（无需认证）

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### 管理员端点

- `POST /texts/upload`
- `GET /users/{id}`

### 限流

- 认证端点：Bucket4j 按 IP 限流（登录 10 次/分，注册 5 次/分，其他 20 次/分）
- 成绩提交：应用层限流，同一用户间隔不少于 5 秒
