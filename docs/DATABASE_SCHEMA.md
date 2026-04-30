# TypeType Database Schema

Self-contained reference for the MySQL schema managed by Flyway migrations V1–V8.

**Database**: `typetype` (MySQL)
**Charset**: utf8mb4
**Migration**: Flyway, auto-runs on application startup

---

## Tables

### t_user

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGINT | no | auto_increment | Primary key |
| username | VARCHAR(50) | no | — | Unique login name |
| password | VARCHAR(100) | no | — | BCrypt hash |
| nickname | VARCHAR(64) | yes | null | Display name |
| avatar_url | VARCHAR(500) | yes | null | Avatar URL |
| role | VARCHAR(20) | no | 'USER' | `USER` or `ADMIN` |
| created_at | DATETIME | no | CURRENT_TIMESTAMP | Creation time |
| updated_at | DATETIME | no | CURRENT_TIMESTAMP ON UPDATE | Last update |

**Indexes**: `UNIQUE (username)`

---

### t_text_source

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGINT | no | auto_increment | Primary key |
| source_key | VARCHAR(50) | no | — | Unique identifier (e.g., `cet4`, `jisubei`) |
| label | VARCHAR(100) | no | — | Human-readable name |
| category | VARCHAR(50) | no | — | `vocabulary`, `article`, or `custom` |
| is_active | TINYINT(1) | no | 1 | Whether source is enabled |
| created_at | DATETIME | no | CURRENT_TIMESTAMP | Creation time |

**Indexes**: `UNIQUE (source_key)`

**Seeded data** (V1, V3, V5):

| source_key | label | category |
|------------|-------|----------|
| cet4 | CET-4 词汇 | vocabulary |
| cet6 | CET-6 词汇 | vocabulary |
| essay_classic | 经典散文 | article |
| code_snippet | 代码片段 | code |
| jisubei | 极速杯 | article |
| custom | 自定义 | custom |

---

### t_text

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGINT | no | auto_increment | Primary key |
| source_id | BIGINT | no | — | FK → t_text_source.id |
| title | VARCHAR(200) | no | — | Text title |
| content | TEXT | no | — | Full text content |
| char_count | INT | no | 0 | Character count (redundant for query speed) |
| difficulty | INT | no | 0 | Difficulty level 0–5 |
| client_text_id | BIGINT | yes | null | Client-computed hash ID |
| created_at | DATETIME | no | CURRENT_TIMESTAMP | Creation time |

**Indexes**: `FK (source_id)`, `UNIQUE (client_text_id)`

**clientTextId computation**: `decimal(SHA-256(sourceKey:content)[0:8]) % 10^9`

---

### t_score

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGINT | no | auto_increment | Primary key |
| user_id | BIGINT | no | — | FK → t_user.id |
| text_id | BIGINT | yes | null | FK → t_text.id |
| char_count | INT | no | 0 | Characters in the text |
| wrong_char_count | INT | no | 0 | Incorrectly typed characters |
| backspace_count | INT | no | 0 | Backspace key presses |
| correction_count | INT | no | 0 | Characters corrected via backspace-then-retype |
| key_stroke_count | INT | no | 0 | Total key presses |
| time | DECIMAL(10,2) | no | 0 | Duration in seconds |
| created_at | DATETIME | no | CURRENT_TIMESTAMP | Submission time |

**Indexes**:
- `idx_user_created` — `(user_id, created_at DESC)` — user history queries
- `idx_created_at` — `(created_at)` — time range queries

**Derived metrics** (computed by `Score` entity getter methods, not stored):

| Metric | Formula | Precision |
|--------|---------|-----------|
| speed | `charCount * 60 / time` | 2 decimals |
| keyStroke | `keyStrokeCount / time` | 2 decimals |
| codeLength | `keyStrokeCount / charCount` | 3 decimals |
| keyAccuracy | `(keyStrokeCount - wrongKeys) / keyStrokeCount * 100` | 2 decimals |
| effectiveSpeed | `(charCount - wrongCharCount) * 60 / time` | 2 decimals |

Where `wrongKeys = backspaceCount + correctionCount * codeLength`.

---

## Migration History

| Version | Description |
|---------|-------------|
| V1 | Create core tables (t_user, t_text_source, t_text, t_score) + seed data |
| V2 | Add `role` column to t_user |
| V3 | Add jisubei text source |
| V4 | Add leaderboard performance indexes |
| V5 | Add custom text source |
| V6 | Add `client_text_id` column to t_text |
| V7 | Add V2 raw fields to t_score (charCount, wrongCharCount, etc.), backfill from legacy |
| V8 | Drop legacy derived columns from t_score (speed, keyStroke, codeLength, etc.) |

---

## Entity-Relationship

```
t_user  1 ──→ N  t_score
t_text  1 ──→ N  t_score
t_text_source  1 ──→ N  t_text
```

- A user submits many scores
- A text belongs to one source, has many scores
- A score links one user to one text (text_id is nullable for offline texts)
