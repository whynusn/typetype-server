-- ==================== TypeType 数据库迁移脚本 ====================
-- 版本：V4
-- 描述：添加排行榜查询所需索引（幂等安全版）

SET @dbname = DATABASE();

-- idx_text_speed：按文本分组、速度降序
SELECT COUNT(*) INTO @exists FROM information_schema.statistics
WHERE table_schema = @dbname AND table_name = 't_score' AND index_name = 'idx_text_speed';
SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_text_speed ON t_score(text_id, speed DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- idx_text_user_created：用户在某文本的成绩查询索引
SELECT COUNT(*) INTO @exists FROM information_schema.statistics
WHERE table_schema = @dbname AND table_name = 't_score' AND index_name = 'idx_text_user_created';
SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_text_user_created ON t_score(text_id, user_id, created_at DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- idx_text_user_speed：组合索引，用于排行榜子查询优化
SELECT COUNT(*) INTO @exists FROM information_schema.statistics
WHERE table_schema = @dbname AND table_name = 't_score' AND index_name = 'idx_text_user_speed';
SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_text_user_speed ON t_score(text_id, user_id, speed DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
