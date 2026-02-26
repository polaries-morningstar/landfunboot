ALTER TABLE sys_user CHANGE COLUMN enabled is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用(0:禁用, 1:启用)';
ALTER TABLE sys_user CHANGE COLUMN deleted delete_time BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除时间(0:未删除, 毫秒值:已删除)';
