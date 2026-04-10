-- Add client_text_id field for client-generated text hash
ALTER TABLE t_text ADD COLUMN client_text_id BIGINT UNIQUE COMMENT '客户端文本ID（hash）';
CREATE INDEX idx_client_text_id ON t_text(client_text_id);
