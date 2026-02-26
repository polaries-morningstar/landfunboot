-- 1. 先允许 delete_time 为 NULL
ALTER TABLE sys_user MODIFY COLUMN delete_time BIGINT NULL;

-- 2. 将现有的所有 delete_time 设置为 NULL
UPDATE sys_user SET delete_time = NULL;

-- 3. 修改 delete_time 字段类型为 DATETIME(6), 允许 NULL
ALTER TABLE sys_user MODIFY COLUMN delete_time DATETIME(6) NULL DEFAULT NULL COMMENT '逻辑删除时间(NULL:未删除, 时间戳:已删除)';

-- 4. 删除旧的唯一约束 (如果存在)
ALTER TABLE sys_user DROP INDEX uk_username;
ALTER TABLE sys_user DROP INDEX uk_email;

-- 5. 添加包含逻辑删除字段的复合唯一约束
ALTER TABLE sys_user ADD UNIQUE INDEX uk_username_delete_time (username, delete_time);
ALTER TABLE sys_user ADD UNIQUE INDEX uk_email_delete_time (email, delete_time);
