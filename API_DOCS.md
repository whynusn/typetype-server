# TypeType Server API Documentation

## Table of Contents
1. [API Overview](#api-overview)
2. [Authentication Endpoints](#authentication-endpoints)
3. [Text Management Endpoints](#text-management-endpoints)
4. [Score Management Endpoints](#score-management-endpoints)
5. [User Management Endpoints](#user-management-endpoints)
6. [Data Models](#data-models)
7. [Security Configuration](#security-configuration)

## API Overview

- **Base URL**: `/api/v1`
- **Authentication**: JWT Bearer Token (except for auth endpoints)
- **Response Format**: All responses are wrapped in `Result<T>` object
- **Date Format**: ISO 8601 (e.g., `2024-01-01T00:00:00`)

### Common Response Structure

```json
{
  "code": 200,
  "message": "Success message",
  "data": { ... },
  "timestamp": 1704067200000
}
```

### Error Codes
- `200`: Success
- `10001`: System error
- `10002`: Parameter validation failed
- `10003`: Resource not found
- `20001`: Token expired
- `20002`: Token invalid
- `20003`: Password error
- `20004`: Username not found
- `20005`: Username already exists
- `30001`: Text source not found
- `30002`: No available text
- `40001`: Score data invalid
- `40002`: Submit too frequent

---

## Authentication Endpoints

### 1. User Registration
- **URL**: `POST /api/v1/auth/register`
- **Description**: Register a new user
- **Authentication**: Not required
- **Request Body**:
```json
{
  "username": "john_doe",          // Required, 3-20 chars, alphanumeric + underscore
  "password": "password123",       // Required, 6-30 chars
  "confirmPassword": "password123", // Required
  "nickname": "John Doe"           // Optional, max 64 chars
}
```
- **Response**: `Result<UserVO>` (see UserVO in Data Models)

### 2. User Login
- **URL**: `POST /api/v1/auth/login`
- **Description**: Login with username and password
- **Authentication**: Not required
- **Request Body**:
```json
{
  "username": "john_doe",    // Required
  "password": "password123"  // Required
}
```
- **Response**: `Result<TokenVO>` (see TokenVO in Data Models)

### 3. Refresh Token
- **URL**: `POST /api/v1/auth/refresh`
- **Description**: Refresh access token using refresh token
- **Authentication**: Not required
- **Headers**:
  - `Authorization`: Bearer {refreshToken}
- **Response**: `Result<TokenVO>` (new tokens)

### 4. Logout
- **URL**: `POST /api/v1/auth/logout`
- **Description**: Logout current user
- **Authentication**: Not required
- **Headers**:
  - `Authorization`: Bearer {token} (optional)
- **Response**: `Result<Void>` (success message only)

---

## Text Management Endpoints

### 1. Get Text Catalog
- **URL**: `GET /api/v1/texts/catalog`
- **Description**: Get all active text sources
- **Authentication**: Required
- **Response**: `Result<List<TextSource>>` (see TextSource in Data Models)

### 2. Get Random Text by Source
- **URL**: `GET /api/v1/texts/source/{sourceKey}`
- **Description**: Get a random text from specified source
- **Authentication**: Required
- **Path Parameters**:
  - `sourceKey`: Source identifier (e.g., "cet4", "essay_classic")
- **Response**: `Result<Text>` (see Text in Data Models)

### 3. Get Latest Text by Source
- **URL**: `GET /api/v1/texts/latest/{sourceKey}`
- **Description**: Get the latest text from specified source
- **Authentication**: Required
- **Path Parameters**:
  - `sourceKey`: Source identifier
- **Response**: `Result<Text>`

### 4. Get Text by ID
- **URL**: `GET /api/v1/texts/{id}`
- **Description**: Get text by its ID
- **Authentication**: Required
- **Path Parameters**:
  - `id`: Text ID
- **Response**: `Result<Text>`

### 5. Get Texts by Source
- **URL**: `GET /api/v1/texts/by-source/{sourceKey}`
- **Description**: Get all texts from a specific source
- **Authentication**: Required
- **Path Parameters**:
  - `sourceKey`: Source identifier
- **Response**: `Result<List<Text>>`

### 6. Get Text by Client Text ID
- **URL**: `GET /api/v1/texts/by-client-text-id/{clientTextId}`
- **Description**: Get text by client-generated text ID
- **Authentication**: Required
- **Path Parameters**:
  - `clientTextId`: Client text ID (hash value)
- **Response**: `Result<Text>`

### 7. Upload Text (Admin Only)
- **URL**: `POST /api/v1/texts/upload`
- **Description**: Upload a new text (requires ADMIN role)
- **Authentication**: Required (ADMIN role)
- **Request Body**:
```json
{
  "title": "Text Title",        // Optional
  "content": "Text content...", // Required
  "sourceKey": "custom"         // Required, must be existing source key
}
```
- **Response**: `Result<Text>` (created text)

---

## Score Management Endpoints

### 1. Submit Score
- **URL**: `POST /api/v1/scores`
- **Description**: Submit a typing test score
- **Authentication**: Required
- **Request Body**:
```json
{
  "textId": 123,                    // Optional, server text ID
  "speed": 120.5,                   // Required, speed in chars/min, >= 0
  "effectiveSpeed": 115.3,          // Required, effective speed in chars/min, >= 0
  "keyStroke": 8.5,                 // Required, keystrokes per second, >= 0
  "codeLength": 2.5,               // Required, code length (keystrokes/char), >= 0
  "accuracyRate": 98.5,            // Required, accuracy percentage, 0-100
  "charCount": 500,                // Required, total characters, >= 0
  "wrongCharCount": 5,             // Required, wrong characters, >= 0
  "duration": 120.0                // Required, duration in seconds, >= 0
}
```
- **Response**: `Result<Void>` (success message)

### 2. Get User Score History
- **URL**: `GET /api/v1/scores/history`
- **Description**: Get current user's score history
- **Authentication**: Required
- **Query Parameters**:
  - `page`: Page number (default: 1)
  - `size`: Page size (default: 20)
- **Response**: `Result<PageResult<ScoreVO>>` (see ScoreVO in Data Models)

### 3. Get User Text Score History
- **URL**: `GET /api/v1/texts/{textId}/scores`
- **Description**: Get current user's scores for a specific text
- **Authentication**: Required
- **Path Parameters**:
  - `textId`: Text ID
- **Query Parameters**:
  - `page`: Page number (default: 1)
  - `size`: Page size (default: 20)
- **Response**: `Result<PageResult<ScoreVO>>`

### 4. Get Text Leaderboard
- **URL**: `GET /api/v1/texts/{textId}/leaderboard`
- **Description**: Get leaderboard for a specific text
- **Authentication**: Required
- **Path Parameters**:
  - `textId`: Text ID
- **Query Parameters**:
  - `page`: Page number (default: 1)
  - `size`: Page size (default: 50)
- **Response**: `Result<PageResult<LeaderboardVO>>` (see LeaderboardVO in Data Models)

### 5. Get User Best Score for Text
- **URL**: `GET /api/v1/texts/{textId}/best`
- **Description**: Get current user's best score for a specific text
- **Authentication**: Required
- **Path Parameters**:
  - `textId`: Text ID
- **Response**: `Result<ScoreVO>` (can be null if no scores)

---

## User Management Endpoints

### 1. Get Current User
- **URL**: `GET /api/v1/users/me`
- **Description**: Get current authenticated user's information
- **Authentication**: Required
- **Response**: `Result<UserVO>` (see UserVO in Data Models)

### 2. Get User by ID (Admin Only)
- **URL**: `GET /api/v1/users/{id}`
- **Description**: Get user by ID (requires ADMIN role)
- **Authentication**: Required (ADMIN role)
- **Path Parameters**:
  - `id`: User ID
- **Response**: `Result<UserVO>`

---

## Data Models

### DTOs (Data Transfer Objects)

#### RegisterDTO
| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| username | String | Required, 3-20 chars, alphanumeric + underscore | Username |
| password | String | Required, 6-30 chars | Password |
| confirmPassword | String | Required | Password confirmation |
| nickname | String | Optional, max 64 chars | User nickname |

#### LoginDTO
| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| username | String | Required | Username |
| password | String | Required | Password |

#### TokenVO
| Field | Type | Description |
|-------|------|-------------|
| accessToken | String | JWT access token |
| refreshToken | String | JWT refresh token |
| expiresIn | Long | Access token expiration time in seconds |
| user | UserVO | User information |

#### JwtPayloadDTO
| Field | Type | Description |
|-------|------|-------------|
| userId | Long | User ID |
| username | String | Username |
| tokenType | String | Token type (access/refresh) |
| role | String | User role (USER/ADMIN) |
| iat | Long | Issued at timestamp (ms) |
| exp | Long | Expiration timestamp (ms) |
| iss | String | Issuer |
| sub | String | Subject |

#### UploadTextDTO
| Field | Type | Description |
|-------|------|-------------|
| title | String | Text title |
| content | String | Text content |
| sourceKey | String | Source key |

#### FetchedTextDTO
| Field | Type | Description |
|-------|------|-------------|
| title | String | Article title |
| content | String | Article content |

#### SubmitScoreDTO
| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| textId | Long | Optional | Text ID |
| speed | BigDecimal | Required, >= 0 | Speed (chars/min) |
| effectiveSpeed | BigDecimal | Required, >= 0 | Effective speed (chars/min) |
| keyStroke | BigDecimal | Required, >= 0 | Keystrokes per second |
| codeLength | BigDecimal | Required, >= 0 | Code length (keystrokes/char) |
| accuracyRate | BigDecimal | Required, 0-100 | Accuracy percentage |
| charCount | Integer | Required, >= 0 | Character count |
| wrongCharCount | Integer | Required, >= 0 | Wrong character count |
| duration | BigDecimal | Required, >= 0 | Duration (seconds) |

### VOs (View Objects)

#### UserVO
| Field | Type | Description |
|-------|------|-------------|
| id | Long | User ID |
| username | String | Username |
| nickname | String | User nickname |
| avatarUrl | String | Avatar URL |
| createdAt | LocalDateTime | Creation time |
| updatedAt | LocalDateTime | Update time |

#### ScoreVO
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Score ID |
| textId | Long | Text ID |
| textTitle | String | Text title |
| speed | BigDecimal | Speed (chars/min) |
| effectiveSpeed | BigDecimal | Effective speed (chars/min) |
| keyStroke | BigDecimal | Keystrokes per second |
| codeLength | BigDecimal | Code length |
| accuracyRate | BigDecimal | Accuracy percentage |
| charCount | Integer | Character count |
| wrongCharCount | Integer | Wrong character count |
| duration | BigDecimal | Duration (seconds) |
| createdAt | LocalDateTime | Creation time |

#### LeaderboardVO
| Field | Type | Description |
|-------|------|-------------|
| rank | Integer | Ranking position |
| userId | Long | User ID |
| username | String | Username |
| nickname | String | User nickname |
| avatarUrl | String | Avatar URL |
| speed | BigDecimal | Speed (chars/min) |
| effectiveSpeed | BigDecimal | Effective speed (chars/min) |
| keyStroke | BigDecimal | Keystrokes per second |
| codeLength | BigDecimal | Code length |
| accuracyRate | BigDecimal | Accuracy percentage |
| charCount | Integer | Character count |
| wrongCharCount | Integer | Wrong character count |
| duration | BigDecimal | Duration (seconds) |
| createdAt | LocalDateTime | Achievement time |

### Entities

#### User
| Field | Type | Description |
|-------|------|-------------|
| id | Long | User ID (PK) |
| username | String | Username (unique) |
| password | String | BCrypt encrypted password |
| nickname | String | User nickname |
| avatarUrl | String | Avatar URL |
| role | String | User role (USER/ADMIN) |
| createdAt | LocalDateTime | Creation time |
| updatedAt | LocalDateTime | Update time |

#### Text
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Text ID (PK) |
| sourceId | Long | Source ID (FK to t_text_source) |
| title | String | Text title |
| content | String | Text content |
| charCount | Integer | Character count (redundant) |
| difficulty | Integer | Difficulty level (0-5) |
| clientTextId | Long | Client text ID (hash) |
| createdAt | LocalDateTime | Creation time |

#### TextSource
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Source ID (PK) |
| sourceKey | String | Source identifier (unique) |
| label | String | Source name |
| category | String | Category (vocabulary/article/custom) |
| isActive | Boolean | Is active |
| createdAt | LocalDateTime | Creation time |

#### Score
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Score ID (PK) |
| userId | Long | User ID (FK to t_user) |
| textId | Long | Text ID (FK to t_text, optional) |
| speed | BigDecimal | Speed (chars/min) |
| effectiveSpeed | BigDecimal | Effective speed (chars/min) |
| keyStroke | BigDecimal | Keystrokes per second |
| codeLength | BigDecimal | Code length |
| accuracyRate | BigDecimal | Accuracy percentage |
| charCount | Integer | Character count |
| wrongCharCount | Integer | Wrong character count |
| duration | BigDecimal | Duration (seconds) |
| createdAt | LocalDateTime | Creation time |
| textTitle | String | Text title (joined field, not in DB) |

### Common Response Wrappers

#### Result<T>
| Field | Type | Description |
|-------|------|-------------|
| code | Integer | Status code |
| message | String | Response message |
| data | T | Response data (generic) |
| timestamp | Long | Response timestamp (ms) |

#### PageResult<T>
| Field | Type | Description |
|-------|------|-------------|
| records | List<T> | Data records |
| total | Long | Total record count |
| page | Long | Current page number |
| size | Long | Page size |
| pages | Long | Total page count |

---

## Security Configuration

### Public Endpoints (No Authentication Required)
- `POST /api/v1/auth/**` - All authentication endpoints
  - `/api/v1/auth/register`
  - `/api/v1/auth/login`
  - `/api/v1/auth/refresh`
  - `/api/v1/auth/logout`
- `GET /api/v1/health` - Health check (if implemented)
- `/error` - Default error page

### Protected Endpoints (Authentication Required)
All other endpoints require JWT authentication in the Authorization header:
```
Authorization: Bearer {accessToken}
```

### Role-Based Access Control
- **ADMIN Role Required**:
  - `POST /api/v1/texts/upload` - Upload text
  - `GET /api/v1/users/{id}` - Get user by ID

### JWT Configuration
- **Algorithm**: HS256 (assumed based on common practice)
- **Secret Key**: Read from `JWT_SECRET_KEY` environment variable
- **Token Types**:
  - Access Token: Short-lived (typically 15 minutes)
  - Refresh Token: Long-lived (typically 7 days)

### Authentication Flow
1. User registers or logs in to get tokens
2. Client includes access token in Authorization header
3. JwtAuthenticationFilter validates token on each request
4. If access token expires, client uses refresh token to get new tokens
5. On logout, client deletes tokens locally (server may invalidate refresh token)

---

## Additional Notes

1. **Database**: MySQL with MyBatis ORM
2. **Migrations**: Flyway manages database schema changes
3. **Password Storage**: BCrypt with strength 10
4. **Validation**: Jakarta Bean Validation with custom error handling
5. **Error Handling**: Global exception handler returns standardized error responses

### API Versioning
All endpoints are under `/api/v1/` prefix for future versioning support.

### Rate Limiting
Score submission has built-in frequency protection (`SCORE_SUBMIT_TOO_FREQUENT` error code).