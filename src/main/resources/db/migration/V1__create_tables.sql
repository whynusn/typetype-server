-- ==================== TypeType 数据库初始化脚本 ====================
-- 版本：V1
-- 描述：创建用户、文本来源、文本、成绩四张表

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(32) UNIQUE NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    nickname    VARCHAR(64) COMMENT '昵称',
    avatar_url  VARCHAR(512) COMMENT '头像URL',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 文本来源表
CREATE TABLE IF NOT EXISTS t_text_source (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '来源ID',
    source_key  VARCHAR(64) UNIQUE NOT NULL COMMENT '来源标识（如 cet4、essay_classic）',
    label       VARCHAR(128) NOT NULL COMMENT '来源名称',
    category    VARCHAR(32) NOT NULL COMMENT '分类（vocabulary/article/custom）',
    is_active   TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文本来源表';

-- 文本表
CREATE TABLE IF NOT EXISTS t_text (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文本ID',
    source_id   BIGINT NOT NULL COMMENT '来源ID',
    title       VARCHAR(255) COMMENT '文本标题',
    content     TEXT NOT NULL COMMENT '文本内容',
    char_count  INT NOT NULL COMMENT '字符数（冗余字段）',
    difficulty  TINYINT DEFAULT 0 COMMENT '难度等级（0-5）',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (source_id) REFERENCES t_text_source(id),
    INDEX idx_source_difficulty (source_id, difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文本表';

-- 成绩表
CREATE TABLE IF NOT EXISTS t_score (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成绩ID',
    user_id           BIGINT NOT NULL COMMENT '用户ID',
    text_id           BIGINT COMMENT '文本ID',
    speed             DECIMAL(8,2) NOT NULL COMMENT '速度（字/分）',
    effective_speed   DECIMAL(8,2) NOT NULL COMMENT '有效速度（字/分）',
    key_stroke        DECIMAL(8,2) NOT NULL COMMENT '击键速度（击/秒）',
    code_length       DECIMAL(8,4) NOT NULL COMMENT '码长（击/字）',
    accuracy_rate     DECIMAL(5,2) NOT NULL COMMENT '准确率（%）',
    char_count        INT NOT NULL COMMENT '字符数',
    wrong_char_count  INT NOT NULL COMMENT '错误字符数',
    duration          DECIMAL(10,2) NOT NULL COMMENT '时长（秒）',
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_speed (speed DESC),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';

-- 插入示例文本来源
INSERT INTO t_text_source (source_key, label, category, is_active)
VALUES
    ('cet4', 'CET-4 词汇', 'vocabulary', 1),
    ('cet6', 'CET-6 词汇', 'vocabulary', 1),
    ('essay_classic', '经典散文', 'article', 1),
    ('code_snippet', '代码片段', 'custom', 1)
ON DUPLICATE KEY UPDATE source_key = source_key;

-- 插入示例文本
INSERT INTO t_text (source_id, title, content, char_count, difficulty)
SELECT
    (SELECT id FROM t_text_source WHERE source_key = 'cet4' LIMIT 1),
    'CET-4 Vocabulary Practice Test',
    'Welcome to the CET-4 vocabulary practice test. Type the following words carefully and accurately. Remember that speed is important, but accuracy is even more crucial. Take a deep breath and focus on the keyboard.',
    LENGTH('Welcome to the CET-4 vocabulary practice test. Type the following words carefully and accurately. Remember that speed is important, but accuracy is even more crucial. Take a deep breath and focus on the keyboard.'),
    3
ON DUPLICATE KEY UPDATE id = id;

-- 再插入几条 CET-6 文本
INSERT INTO t_text (source_id, title, content, char_count, difficulty)
SELECT
    (SELECT id FROM t_text_source WHERE source_key = 'cet6' LIMIT 1),
    'CET-6 Reading Comprehension',
    'The concept of artificial intelligence has evolved significantly over the past few decades. Researchers have developed various approaches to machine learning, enabling computers to perform tasks that were once thought to be exclusively human.',
    LENGTH('The concept of artificial intelligence has evolved significantly over the past few decades. Researchers have developed various approaches to machine learning, enabling computers to perform tasks that were once thought to be exclusively human.'),
    4
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_text (source_id, title, content, char_count, difficulty)
SELECT
    (SELECT id FROM t_text_source WHERE source_key = 'cet6' LIMIT 1),
    'CET-6 Academic Writing',
    'Scholarly articles typically follow a specific structure: introduction, literature review, methodology, results, discussion, and conclusion. Each section serves a distinct purpose in presenting research findings clearly and systematically.',
    LENGTH('Scholarly articles typically follow a specific structure: introduction, literature review, methodology, results, discussion, and conclusion. Each section serves a distinct purpose in presenting research findings clearly and systematically.'),
    4
ON DUPLICATE KEY UPDATE id = id;
