-- 对齐客户端成绩指标契约（V2）
-- 新增原始字段：char_count, wrong_char_count, backspace_count, correction_count, key_stroke_count, time
-- 删除兼容字段：speed, key_stroke, code_length, key_accuracy, duration, effective_speed, accuracy_rate

-- 添加字符数列（原始字段）
ALTER TABLE t_score
ADD COLUMN char_count INT NOT NULL DEFAULT 0 COMMENT '字符数';

-- 添加错误字符数列（原始字段）
ALTER TABLE t_score
ADD COLUMN wrong_char_count INT NOT NULL DEFAULT 0 COMMENT '错误字符数';

-- 添加退格数列（原始字段）
ALTER TABLE t_score
ADD COLUMN backspace_count INT NOT NULL DEFAULT 0 COMMENT '退格键按下次数';

-- 添加回改数列（原始字段）
ALTER TABLE t_score
ADD COLUMN correction_count INT NOT NULL DEFAULT 0 COMMENT '回改字数';

-- 添加总按键次数（原始字段）
ALTER TABLE t_score
ADD COLUMN key_stroke_count INT NOT NULL DEFAULT 0 COMMENT '总按键次数';

-- 添加用时列（原始字段，替代 duration）
ALTER TABLE t_score
ADD COLUMN time DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '用时（秒）';

-- 回填历史数据：duration -> time
UPDATE t_score SET time = duration WHERE time = 0 AND duration > 0;

-- 回填历史数据：key_stroke -> key_stroke_count（近似值）
UPDATE t_score SET key_stroke_count = key_stroke * time WHERE key_stroke_count = 0 AND key_stroke > 0 AND time > 0;

-- 回填历史数据：char_count
UPDATE t_score SET char_count = speed * time / 60 WHERE char_count = 0 AND speed > 0 AND time > 0;

-- 回填历史数据：wrong_char_count（从 accuracy_rate 反推）
UPDATE t_score SET wrong_char_count = ROUND(char_count * (100 - accuracy_rate) / 100)
WHERE wrong_char_count = 0 AND char_count > 0 AND accuracy_rate IS NOT NULL;

-- 添加索引
CREATE INDEX idx_user_created ON t_score(user_id, created_at DESC);
CREATE INDEX idx_created_at ON t_score(created_at);