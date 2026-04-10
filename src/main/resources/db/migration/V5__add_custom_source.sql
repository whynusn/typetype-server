-- Add custom text source for user-uploaded texts
INSERT INTO t_text_source (source_key, label, category, is_active, created_at)
VALUES ('custom', '自定义文本', 'custom', 1, NOW())
ON DUPLICATE KEY UPDATE is_active = 1;