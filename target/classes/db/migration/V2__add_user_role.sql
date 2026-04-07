-- ==================== TypeType 数据库迁移脚本 ====================
-- 版本：V2
-- 描述：为用户表增加角色字段

ALTER TABLE t_user
    ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '角色（USER/ADMIN）';

UPDATE t_user
SET role = 'USER'
WHERE role IS NULL OR role = '';
