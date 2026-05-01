-- ==================== 测试数据库初始化脚本 ====================
-- 整合所有 Flyway 迁移的最终状态（V1-V9）
-- 用于 Testcontainers 集成测试

-- 用户表（V1 + V2）
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(32) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(64),
    avatar_url  VARCHAR(512),
    role        VARCHAR(32) NOT NULL DEFAULT 'USER',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 文本来源表（V1）
CREATE TABLE IF NOT EXISTS t_text_source (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_key  VARCHAR(64) UNIQUE NOT NULL,
    label       VARCHAR(128) NOT NULL,
    category    VARCHAR(32) NOT NULL,
    is_active   TINYINT(1) DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 文本表（V1 + V6）
CREATE TABLE IF NOT EXISTS t_text (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_id       BIGINT NOT NULL,
    title           VARCHAR(255),
    content         TEXT NOT NULL,
    char_count      INT NOT NULL,
    difficulty      TINYINT DEFAULT 0,
    client_text_id  BIGINT UNIQUE,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_id) REFERENCES t_text_source(id),
    INDEX idx_source_difficulty (source_id, difficulty),
    INDEX idx_client_text_id (client_text_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 成绩表（V1 + V9 最终状态）
-- V9 移除了旧派生字段（speed, effective_speed, key_stroke, code_length, accuracy_rate, duration）
-- 只保留原始字段，派生指标由 Java getter 计算
CREATE TABLE IF NOT EXISTS t_score (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT NOT NULL,
    text_id           BIGINT,
    char_count        INT NOT NULL DEFAULT 0,
    wrong_char_count  INT NOT NULL DEFAULT 0,
    backspace_count   INT NOT NULL DEFAULT 0,
    correction_count  INT NOT NULL DEFAULT 0,
    key_stroke_count  INT NOT NULL DEFAULT 0,
    `time`            DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_created_at (created_at),
    INDEX idx_text_speed (text_id, (char_count * 60.0 / `time`) DESC),
    INDEX idx_text_user_created (text_id, user_id, created_at DESC),
    INDEX idx_text_user_speed (text_id, user_id, (char_count * 60.0 / `time`) DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置数据
INSERT INTO t_text_source (source_key, label, category, is_active)
VALUES
    ('cet4', 'CET-4 词汇', 'vocabulary', 1),
    ('cet6', 'CET-6 词汇', 'vocabulary', 1),
    ('essay_classic', '经典散文', 'article', 1),
    ('code_snippet', '代码片段', 'custom', 1),
    ('jisubei', '极速杯', 'competition', 1),
    ('custom', '自定义文本', 'custom', 1)
ON DUPLICATE KEY UPDATE source_key = source_key;

-- 测试用文本
INSERT INTO t_text (source_id, title, content, char_count, difficulty)
SELECT
    (SELECT id FROM t_text_source WHERE source_key = 'cet4' LIMIT 1),
    'CET-4 Vocabulary Practice',
    'Welcome to the CET-4 vocabulary practice test.',
    LENGTH('Welcome to the CET-4 vocabulary practice test.'),
    3
ON DUPLICATE KEY UPDATE id = id;
