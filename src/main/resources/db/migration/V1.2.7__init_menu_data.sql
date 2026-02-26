-- Add component and icon columns
-- Add component and icon columns
ALTER TABLE sys_menu ADD COLUMN component VARCHAR(255);
ALTER TABLE sys_menu ADD COLUMN icon VARCHAR(50);
ALTER TABLE sys_menu ADD COLUMN sort_order INT DEFAULT 0;

-- Clear existing data
DELETE FROM sys_role_menu_mapping;
DELETE FROM sys_menu;

-- Reset Auto Increment (Optional, but good for clean slate)
ALTER TABLE sys_menu AUTO_INCREMENT = 1;

-- 1. System Management (Directory)
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, permission, type, sort_order) VALUES
(1, 0, '系统管理', '/system', 'Layout', 'Settings', NULL, 'DIR', 1),
(2, 1, '用户管理', '/system/user', 'system/user/index', 'User', 'sys:user:list', 'MENU', 1),
(3, 1, '角色管理', '/system/role', 'system/role/index', 'Shield', 'sys:role:list', 'MENU', 2),
(4, 1, '部门管理', '/system/dept', 'system/dept/index', 'Network', NULL, 'MENU', 3),
(5, 1, '菜单管理', '/system/menu', 'system/menu/index', 'Menu', NULL, 'MENU', 4);

-- User Buttons
INSERT INTO sys_menu (parent_id, name, permission, type, sort_order) VALUES
(2, '用户新增', 'sys:user:add', 'BUTTON', 1),
(2, '用户修改', 'sys:user:update', 'BUTTON', 2),
(2, '用户删除', 'sys:user:delete', 'BUTTON', 3);

-- Role Buttons
INSERT INTO sys_menu (parent_id, name, permission, type, sort_order) VALUES
(3, '角色查询', 'sys:role:query', 'BUTTON', 1),
(3, '角色新增', 'sys:role:add', 'BUTTON', 2),
(3, '角色修改', 'sys:role:update', 'BUTTON', 3),
(3, '角色删除', 'sys:role:delete', 'BUTTON', 4);

-- Dept Buttons
INSERT INTO sys_menu (parent_id, name, permission, type, sort_order) VALUES
(4, '部门新增', 'sys:dept:add', 'BUTTON', 1),
(4, '部门修改', 'sys:dept:update', 'BUTTON', 2),
(4, '部门删除', 'sys:dept:delete', 'BUTTON', 3);

-- Menu Buttons
INSERT INTO sys_menu (parent_id, name, permission, type, sort_order) VALUES
(5, '菜单新增', 'sys:menu:add', 'BUTTON', 1),
(5, '菜单修改', 'sys:menu:update', 'BUTTON', 2),
(5, '菜单删除', 'sys:menu:delete', 'BUTTON', 3);
