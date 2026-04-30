# TypeType 数据库 Schema

Flyway 迁移 V1–V8 管理的 MySQL schema 自包含参考。

**数据库**: `typetype`（MySQL）
**字符集**: utf8mb4
**迁移方式**: Flyway，应用启动时自动执行

---

## 数据表

### t_user（用户表）

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | 否 | 自增 | 主键 |
| username | VARCHAR(50) | 否 | — | 唯一登录名 |
| password | VARCHAR(100) | 否 | — | BCrypt 哈希值 |
| nickname | VARCHAR(64) | 是 | null | 显示昵称 |
| avatar_url | VARCHAR(500) | 是 | null | 头像 URL |
| role | VARCHAR(20) | 否 | 'USER' | 角色：`USER` 或 `ADMIN` |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 最后更新时间 |

**索引**: `UNIQUE (username)`

---

### t_text_source（文本来源表）

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | 否 | 自增 | 主键 |
| source_key | VARCHAR(50) | 否 | — | 唯一标识符（如 `cet4`、`jisubei`） |
| label | VARCHAR(100) | 否 | — | 人类可读名称 |
| category | VARCHAR(50) | 否 | — | 分类：`vocabulary`、`article`、`code`、`custom` |
| is_active | TINYINT(1) | 否 | 1 | 是否启用 |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

**索引**: `UNIQUE (source_key)`

**预置数据**（V1、V3、V5）：

| source_key | label | category |
|------------|-------|----------|
| cet4 | CET-4 词汇 | vocabulary |
| cet6 | CET-6 词汇 | vocabulary |
| essay_classic | 经典散文 | article |
| code_snippet | 代码片段 | code |
| jisubei | 极速杯 | article |
| custom | 自定义 | custom |

---

### t_text（文本表）

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | 否 | 自增 | 主键 |
| source_id | BIGINT | 否 | — | 外键 → t_text_source.id |
| title | VARCHAR(200) | 否 | — | 文本标题 |
| content | TEXT | 否 | — | 文本内容 |
| char_count | INT | 否 | 0 | 字符数（冗余字段，加速查询） |
| difficulty | INT | 否 | 0 | 难度等级 0–5 |
| client_text_id | BIGINT | 是 | null | 客户端哈希 ID |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

**索引**: `FK (source_id)`、`UNIQUE (client_text_id)`

**clientTextId 计算方式**: `decimal(SHA-256(sourceKey:content)[0:8]) % 10^9`

---

### t_score（成绩表）

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | 否 | 自增 | 主键 |
| user_id | BIGINT | 否 | — | 外键 → t_user.id |
| text_id | BIGINT | 是 | null | 外键 → t_text.id |
| char_count | INT | 否 | 0 | 文本总字符数 |
| wrong_char_count | INT | 否 | 0 | 错误字符数 |
| backspace_count | INT | 否 | 0 | 退格键按下次数 |
| correction_count | INT | 否 | 0 | 回改字数 |
| key_stroke_count | INT | 否 | 0 | 总按键次数 |
| time | DECIMAL(10,2) | 否 | 0 | 用时（秒） |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 提交时间 |

**索引**:
- `idx_user_created` — `(user_id, created_at DESC)` — 用户历史查询
- `idx_created_at` — `(created_at)` — 时间范围查询

**派生指标**（由 Score 实体 getter 方法计算，不存入数据库）：

| 指标 | 公式 | 精度 |
|------|------|------|
| speed | `charCount * 60 / time` | 2 位小数 |
| keyStroke | `keyStrokeCount / time` | 2 位小数 |
| codeLength | `keyStrokeCount / charCount` | 3 位小数 |
| keyAccuracy | `(keyStrokeCount - wrongKeys) / keyStrokeCount * 100` | 2 位小数 |
| effectiveSpeed | `(charCount - wrongCharCount) * 60 / time` | 2 位小数 |

其中 `wrongKeys = backspaceCount + correctionCount * codeLength`。

---

## 迁移历史

| 版本 | 说明 |
|------|------|
| V1 | 创建核心表（t_user、t_text_source、t_text、t_score）+ 预置数据 |
| V2 | t_user 新增 `role` 列 |
| V3 | 新增 jisubei 文本来源 |
| V4 | 排行榜性能索引 |
| V5 | 新增 custom 文本来源 |
| V6 | t_text 新增 `client_text_id` 列 |
| V9 | V2 指标集迁移（幂等安全版）：添加原始字段、回填历史、删除旧派生字段、添加索引 |

---

## 实体关系

```
t_user  1 ──→ N  t_score
t_text  1 ──→ N  score
t_text_source  1 ──→ N  t_text
```

- 一个用户可提交多条成绩
- 一个文本属于一个来源，可有多条成绩
- 一条成绩关联一个用户和一个文本（text_id 可为空，用于离线文本）
