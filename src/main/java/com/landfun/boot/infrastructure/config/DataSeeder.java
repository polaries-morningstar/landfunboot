package com.landfun.boot.infrastructure.config;

import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.landfun.boot.modules.system.menu.Menu;
import com.landfun.boot.modules.system.menu.MenuDraft;
import com.landfun.boot.modules.system.menu.MenuTable;
import com.landfun.boot.modules.system.menu.MenuType;
import com.landfun.boot.modules.system.role.DataScope;
import com.landfun.boot.modules.system.role.Role;
import com.landfun.boot.modules.system.role.RoleDraft;
import com.landfun.boot.modules.system.role.RoleTable;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.user.UserDraft;
import com.landfun.boot.modules.system.user.UserTable;

import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private final JSqlClient sqlClient;
        private final JdbcTemplate jdbcTemplate;

        @Override
        public void run(String... args) {
                log.info("Checking database schema...");
                try {
                        Integer count = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_menu' AND column_name = 'icon'",
                                        Integer.class);
                        if (count == null || count == 0) {
                                log.info("Adding icon column to sys_menu table...");
                                jdbcTemplate.execute(
                                                "ALTER TABLE sys_menu ADD COLUMN icon VARCHAR(255) DEFAULT NULL AFTER name");
                                log.info("Icon column added successfully.");
                        } else {
                                log.info("Icon column already exists.");
                        }
                } catch (Exception e) {
                        log.warn("Manual schema update notice: " + e.getMessage());
                }

                log.info("Seeding data...");
                seedRbac();
        }

        private void seedRbac() {
                // 1. Create Menus
                // User Management
                long userListId = saveMenu(0, "User List", "/sys/user", "UserOutlined", "sys:user:list",
                                MenuType.MENU);
                long userAddId = saveMenu(userListId, "User Add", null, null, "sys:user:add",
                                MenuType.BUTTON);
                long userUpdateId = saveMenu(userListId, "User Update", null, null, "sys:user:update",
                                MenuType.BUTTON);
                long userDeleteId = saveMenu(userListId, "User Delete", null, null, "sys:user:delete",
                                MenuType.BUTTON);
                long userOnlineId = saveMenu(userListId, "Online Users", null, null, "sys:user:online",
                                MenuType.BUTTON);
                long userKickoutId = saveMenu(userListId, "Kickout User", null, null, "sys:user:kickout",
                                MenuType.BUTTON);

                // Role Management
                long roleListId = saveMenu(0, "Role List", "/sys/role", "TeamOutlined", "sys:role:list",
                                MenuType.MENU);
                long roleAddId = saveMenu(roleListId, "Role Add", null, null, "sys:role:add",
                                MenuType.BUTTON);
                long roleUpdateId = saveMenu(roleListId, "Role Update", null, null, "sys:role:update",
                                MenuType.BUTTON);
                long roleDeleteId = saveMenu(roleListId, "Role Delete", null, null, "sys:role:delete",
                                MenuType.BUTTON);

                // Menu Management
                long menuListId = saveMenu(0, "Menu List", "/sys/menu", "MenuOutlined", "sys:menu:list",
                                MenuType.MENU);
                long menuAddId = saveMenu(menuListId, "Menu Add", null, null, "sys:menu:add",
                                MenuType.BUTTON);
                long menuUpdateId = saveMenu(menuListId, "Menu Update", null, null, "sys:menu:update",
                                MenuType.BUTTON);
                long menuDeleteId = saveMenu(menuListId, "Menu Delete", null, null, "sys:menu:delete",
                                MenuType.BUTTON);

                // Dept Management
                long deptListId = saveMenu(0, "Dept List", "/sys/dept", "ApartmentOutlined", "sys:dept:list",
                                MenuType.MENU);
                long deptAddId = saveMenu(deptListId, "Dept Add", null, null, "sys:dept:add",
                                MenuType.BUTTON);
                long deptUpdateId = saveMenu(deptListId, "Dept Update", null, null, "sys:dept:update",
                                MenuType.BUTTON);
                long deptDeleteId = saveMenu(deptListId, "Dept Delete", null, null, "sys:dept:delete",
                                MenuType.BUTTON);

                // System Monitor
                long monitorMenuId = saveMenu(0, "System Monitor", "/sys/monitor", "DashboardOutlined",
                                "sys:monitor:info", MenuType.MENU);

                // 2. Create Roles
                // Admin: All permissions
                long adminRoleId = saveRole("Admin", "admin", "Administrator", DataScope.ALL,
                                java.util.List.of(userListId, userAddId, userUpdateId, userDeleteId,
                                                userOnlineId, userKickoutId,
                                                roleListId, roleAddId, roleUpdateId, roleDeleteId,
                                                menuListId, menuAddId, menuUpdateId, menuDeleteId,
                                                deptListId, deptAddId, deptUpdateId, deptDeleteId,
                                                monitorMenuId));

                // ReadOnly: List only
                long readOnlyRoleId = saveRole("ReadOnly", "readonly", "Read Only User", DataScope.ALL,
                                java.util.List.of(userListId, roleListId, menuListId));

                // 3. Update Admin User
                updateAdminUser(adminRoleId);
                log.info("RBAC Data seeded successfully.");
        }

        private long saveMenu(long parentId, String name, String path, String icon, String permission,
                        MenuType type) {
                MenuTable t = MenuTable.$;
                Menu existing = null;
                if (permission != null) {
                        existing = sqlClient.createQuery(t)
                                        .where(t.permission().eq(permission))
                                        .select(t)
                                        .fetchOneOrNull();
                } else if (path != null) {
                        existing = sqlClient.createQuery(t)
                                        .where(t.path().eq(path))
                                        .select(t)
                                        .fetchOneOrNull();
                } else {
                        existing = sqlClient.createQuery(t)
                                        .where(t.parentId().eq(parentId), t.name().eq(name))
                                        .select(t)
                                        .fetchOneOrNull();
                }

                if (existing != null) {
                        return existing.id();
                }

                return sqlClient.getEntities().save(
                                MenuDraft.$.produce(draft -> {
                                        draft.setParentId(parentId);
                                        draft.setName(name);
                                        draft.setPath(path);
                                        draft.setIcon(icon);
                                        draft.setPermission(permission);
                                        draft.setType(type);
                                        // draft.setSortOrder(0);
                                })).getModifiedEntity().id();
        }

        private long saveRole(String name, String code, String description, DataScope dataScope,
                        java.util.List<Long> menuIds) {
                Role existing = sqlClient.createQuery(RoleTable.$)
                                .where(RoleTable.$.code().eq(code))
                                .select(RoleTable.$)
                                .fetchOneOrNull();

                return sqlClient.getEntities().save(
                                RoleDraft.$.produce(draft -> {
                                        if (existing != null) {
                                                draft.setId(existing.id());
                                        }
                                        draft.setName(name);
                                        draft.setCode(code);
                                        draft.setDescription(description);
                                        draft.setDataScope(dataScope);
                                        // Assign menus by ID
                                        for (Long menuId : menuIds) {
                                                draft.addIntoMenus(menu -> menu.setId(menuId));
                                        }
                                })).getModifiedEntity().id();
        }

        private void updateAdminUser(long roleId) {
                String email = "admin@landfun.com";
                UserTable t = UserTable.$;
                User existing = sqlClient.createQuery(t)
                                .where(t.email().eq(email))
                                .select(t)
                                .fetchOneOrNull();

                if (existing == null) {
                        sqlClient.getEntities().save(
                                        UserDraft.$.produce(draft -> {
                                                draft.setUsername("admin");
                                                draft.setEmail(email);
                                                draft.setPassword(BCrypt.hashpw("password"));
                                                draft.setActive(true);
                                                draft.addIntoRoles(role -> role.setId(roleId));
                                        }));
                } else {
                        // Attach role if not present? Or just force overwrite roles?
                        // Simple: Force assign admin role
                        sqlClient.getEntities().saveCommand(
                                        UserDraft.$.produce(existing, draft -> {
                                                draft.setActive(true);
                                                draft.addIntoRoles(role -> role.setId(roleId));
                                        })).execute();
                }
        }
}
