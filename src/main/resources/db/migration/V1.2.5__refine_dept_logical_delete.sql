-- 1. 先允许 delete_time 为 NULL (通过重命名或修改列)
-- 之前的列名是 deleted_millis BIGINT DEFAULT 0
ALTER TABLE sys_dept CHANGE COLUMN deleted_millis delete_time DATETIME(6) NULL DEFAULT NULL COMMENT '逻辑删除时间(NULL:未删除, 时间戳:已删除)';

-- 2. 将所有现有数据的 delete_time 设置为 NULL
UPDATE sys_dept SET delete_time = NULL;

-- 3. 添加基于名称和逻辑删除状态的唯一索引
ALTER TABLE sys_dept ADD UNIQUE INDEX uk_name_delete_time (name, delete_time);
