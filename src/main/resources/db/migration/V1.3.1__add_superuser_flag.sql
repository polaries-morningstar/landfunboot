ALTER TABLE sys_user ADD COLUMN is_superuser tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为超级管理员(1-是, 0-否)';
UPDATE sys_user SET is_superuser = 1 WHERE username = 'admin';
