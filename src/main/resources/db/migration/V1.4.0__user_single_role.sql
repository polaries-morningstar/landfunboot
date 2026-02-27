-- Migrate user-role relation from many-to-many to single-role model

SET FOREIGN_KEY_CHECKS = 0;

-- Drop old user-role mapping table if it exists
DROP TABLE IF EXISTS sys_user_role_mapping;

-- Add role_id foreign key column to sys_user if not exists
ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS role_id BIGINT UNSIGNED NULL AFTER is_superuser;

-- Add foreign key constraint for role_id (ignore error if it already exists)
ALTER TABLE sys_user
    ADD CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id);

-- Optional: Reset RBAC data so that DataSeeder can recreate a clean set
TRUNCATE TABLE sys_role_menu_mapping;
TRUNCATE TABLE sys_menu;
TRUNCATE TABLE sys_role;

SET FOREIGN_KEY_CHECKS = 1;

