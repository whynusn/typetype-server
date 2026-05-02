-- ==================== TypeType 数据库迁移脚本 ====================
-- 版本：V2
-- 描述：为用户表增加角色字段（幂等安全版）

SET @dbname = DATABASE();

-- role 列（如不存在则添加）
SELECT COUNT(*) INTO @exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 't_user' AND column_name = 'role';
SET @sql = IF(@exists = 0,
    'ALTER TABLE t_user ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT \'USER\' COMMENT \'角色（USER/ADMIN）\'',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE t_user
SET role = 'USER'
WHERE role IS NULL OR role = '';
