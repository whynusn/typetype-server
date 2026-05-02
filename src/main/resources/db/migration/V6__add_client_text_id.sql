-- ==================== TypeType 数据库迁移脚本 ====================
-- 版本：V6
-- 描述：添加客户端文本ID字段（幂等安全版）

SET @dbname = DATABASE();

-- client_text_id 列（如不存在则添加）
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_text' AND column_name = 'client_text_id';
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_text ADD COLUMN client_text_id BIGINT UNIQUE COMMENT \'客户端文本ID（hash）\'',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- idx_client_text_id 索引（如不存在则添加）
SELECT COUNT(*) INTO @exists FROM information_schema.statistics
WHERE table_schema = @dbname AND table_name = 't_text' AND index_name = 'idx_client_text_id';
SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_client_text_id ON t_text(client_text_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
