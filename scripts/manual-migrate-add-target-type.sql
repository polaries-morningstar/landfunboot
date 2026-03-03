-- 当出现 Unknown column 'target_type' 时，可手动执行本脚本为 sys_message 表添加 target_type 列。
--
-- 执行方式（在项目根目录，请将 YOUR_ROOT_PASSWORD 换成实际密码）：
--   docker exec -i landfun-mysql mysql -uroot -pYOUR_ROOT_PASSWORD landfunboot < scripts/manual-migrate-add-target-type.sql
--
-- 若列已存在会报 Duplicate column name 'target_type'，可忽略。

USE landfunboot;

ALTER TABLE sys_message ADD COLUMN target_type VARCHAR(32) NULL;
