-- ==================== TypeType 数据库迁移脚本 ====================
-- 版本：V4
-- 描述：添加排行榜查询所需索引

-- 排行榜核心索引：按文本分组、速度降序
-- 用于：查询某文本的排行榜（ORDER BY speed DESC）
CREATE INDEX idx_text_speed ON t_score(text_id, speed DESC);

-- 用户在某文本的成绩查询索引
-- 用于：查询用户在特定文本的历史成绩
CREATE INDEX idx_text_user_created ON t_score(text_id, user_id, created_at DESC);

-- 组合索引：用于排行榜子查询优化
-- 用于：每用户取最佳成绩的 GROUP BY 查询
CREATE INDEX idx_text_user_speed ON t_score(text_id, user_id, speed DESC);
