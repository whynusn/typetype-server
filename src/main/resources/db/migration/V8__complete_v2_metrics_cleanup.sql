-- V2 指标集完整迁移：只保留原始字段，删除所有派生字段存储
-- 原始字段：char_count, wrong_char_count, backspace_count, correction_count, key_stroke_count, time
-- 删除所有派生/兼容字段：speed, key_stroke, code_length, key_accuracy, duration, effective_speed, accuracy_rate
-- 注意：数据已回填到新字段

-- 请确保运行此脚本在生产环境下执行前已备份数据

-- 删除旧兼容字段
ALTER TABLE t_score DROP COLUMN duration;
ALTER TABLE t_score DROP COLUMN effective_speed;
ALTER TABLE t_score DROP COLUMN accuracy_rate;

-- 删除派生字段（不再存储，改为计算生成）
ALTER TABLE t_score DROP COLUMN speed;
ALTER TABLE t_score DROP COLUMN key_stroke;
ALTER TABLE t_score DROP COLUMN code_length;
ALTER TABLE t_score DROP COLUMN key_accuracy;
