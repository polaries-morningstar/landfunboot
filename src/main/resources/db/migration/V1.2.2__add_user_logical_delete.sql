ALTER TABLE sys_user ADD COLUMN deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记(0:未删除, 1:已删除)';
