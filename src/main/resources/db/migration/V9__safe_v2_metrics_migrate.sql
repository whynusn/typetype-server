-- V2 指标集迁移（幂等安全版）
-- 替代已删除的 V7/V8，统一处理列的添加和删除
-- 所有操作均检查存在性，可安全重复执行

-- ========== 添加原始字段（如不存在则添加） ==========

SET @dbname = DATABASE();

-- char_count
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'char_count';
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_score ADD COLUMN char_count INT NOT NULL DEFAULT 0 COMMENT ''字符数''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- wrong_char_count
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'wrong_char_count';
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_score ADD COLUMN wrong_char_count INT NOT NULL DEFAULT 0 COMMENT ''错误字符数''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- backspace_count
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'backspace_count';
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_score ADD COLUMN backspace_count INT NOT NULL DEFAULT 0 COMMENT ''退格键按下次数''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- correction_count
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'correction_count';
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_score ADD COLUMN correction_count INT NOT NULL DEFAULT 0 COMMENT ''回改字数''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- key_stroke_count
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'key_stroke_count';
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_score ADD COLUMN key_stroke_count INT NOT NULL DEFAULT 0 COMMENT ''总按键次数''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- time
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'time';
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_score ADD COLUMN `time` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT ''用时（秒）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 回填历史数据（仅对新列值为 0 的行回填） ==========

UPDATE t_score SET `time` = duration WHERE `time` = 0 AND duration > 0;
UPDATE t_score SET key_stroke_count = ROUND(key_stroke * `time`) WHERE key_stroke_count = 0 AND key_stroke > 0 AND `time` > 0;
UPDATE t_score SET char_count = ROUND(speed * `time` / 60) WHERE char_count = 0 AND speed > 0 AND `time` > 0;
UPDATE t_score SET wrong_char_count = ROUND(char_count * (100 - accuracy_rate) / 100)
WHERE wrong_char_count = 0 AND char_count > 0 AND accuracy_rate IS NOT NULL;

-- ========== 删除旧派生字段（如存在则删除） ==========

-- duration
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'duration';
SET @sql = IF(@exists > 0, 'ALTER TABLE t_score DROP COLUMN duration', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- effective_speed
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'effective_speed';
SET @sql = IF(@exists > 0, 'ALTER TABLE t_score DROP COLUMN effective_speed', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- accuracy_rate
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'accuracy_rate';
SET @sql = IF(@exists > 0, 'ALTER TABLE t_score DROP COLUMN accuracy_rate', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- speed
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'speed';
SET @sql = IF(@exists > 0, 'ALTER TABLE t_score DROP COLUMN speed', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- key_stroke
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'key_stroke';
SET @sql = IF(@exists > 0, 'ALTER TABLE t_score DROP COLUMN key_stroke', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- code_length
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'code_length';
SET @sql = IF(@exists > 0, 'ALTER TABLE t_score DROP COLUMN code_length', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- key_accuracy
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_score' AND column_name = 'key_accuracy';
SET @sql = IF(@exists > 0, 'ALTER TABLE t_score DROP COLUMN key_accuracy', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 添加索引（如不存在则添加） ==========

SELECT COUNT(*) INTO @exists FROM information_schema.statistics
WHERE table_schema = @dbname AND table_name = 't_score' AND index_name = 'idx_user_created';
SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_user_created ON t_score(user_id, created_at DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists FROM information_schema.statistics
WHERE table_schema = @dbname AND table_name = 't_score' AND index_name = 'idx_created_at';
SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_created_at ON t_score(created_at)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
