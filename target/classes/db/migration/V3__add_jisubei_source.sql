-- V3: 添加极速杯文本来源
INSERT INTO t_text_source (source_key, label, category, is_active)
VALUES
    ('jisubei', '极速杯', 'competition', 1)
ON DUPLICATE KEY UPDATE source_key = source_key;
