package com.landfun.boot.infrastructure.config;

import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.landfun.boot.modules.system.dept.Dept;
import com.landfun.boot.modules.system.dept.DeptDraft;
import com.landfun.boot.modules.system.dept.DeptTable;
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
                log.info("Seeding data...");
                seedRbac();
        }

        private void seedRbac() {
                // 1. Create Menus
                // Group 1: 系统管理 (DIR)
                long sysDirId = saveMenu(0, "系统管理", null, "settings", "DIR_SYS_ADMIN", MenuType.DIR);

                // 1.1 用户管理 (MENU)
                long userListId = saveMenu(sysDirId, "用户管理", "/system/user", "user", "sys:user:list", MenuType.MENU);
                long userAddId = saveMenu(userListId, "用户新增", null, null, "sys:user:add", MenuType.BUTTON);
                long userUpdateId = saveMenu(userListId, "用户修改", null, null, "sys:user:update", MenuType.BUTTON);
                long userDeleteId = saveMenu(userListId, "用户删除", null, null, "sys:user:delete", MenuType.BUTTON);

                // 1.2 角色管理 (MENU)
                long roleListId = saveMenu(sysDirId, "角色管理", "/system/role", "role", "sys:role:list", MenuType.MENU);
                long roleQueryId = saveMenu(roleListId, "角色查询", null, null, "sys:role:query", MenuType.BUTTON);
                long roleAddId = saveMenu(roleListId, "角色新增", null, null, "sys:role:add", MenuType.BUTTON);
                long roleUpdateId = saveMenu(roleListId, "角色修改", null, null, "sys:role:update", MenuType.BUTTON);
                long roleDeleteId = saveMenu(roleListId, "角色删除", null, null, "sys:role:delete", MenuType.BUTTON);

                // 1.3 部门管理 (MENU)
                long deptListId = saveMenu(sysDirId, "部门管理", "/system/dept", "dept", "sys:dept:list", MenuType.MENU);
                long deptAddId = saveMenu(deptListId, "部门新增", null, null, "sys:dept:add", MenuType.BUTTON);
                long deptUpdateId = saveMenu(deptListId, "部门修改", null, null, "sys:dept:update", MenuType.BUTTON);
                long deptDeleteId = saveMenu(deptListId, "部门删除", null, null, "sys:dept:delete", MenuType.BUTTON);

                // 1.4 菜单管理 (MENU)
                long menuListId = saveMenu(sysDirId, "菜单管理", "/system/menu", "menu", "sys:menu:list", MenuType.MENU);
                long menuAddId = saveMenu(menuListId, "菜单新增", null, null, "sys:menu:add", MenuType.BUTTON);
                long menuUpdateId = saveMenu(menuListId, "菜单修改", null, null, "sys:menu:update", MenuType.BUTTON);
                long menuDeleteId = saveMenu(menuListId, "菜单删除", null, null, "sys:menu:delete", MenuType.BUTTON);

                // Group 2: 系统监控 (DIR)
                long monitorDirId = saveMenu(0, "系统监控", null, "monitor", "DIR_SYS_MONITOR", MenuType.DIR);

                // 2.1 服务监控 (MENU)
                long monitorMenuId = saveMenu(monitorDirId, "服务监控", "/monitor", "monitor", "sys:monitor:info",
                                MenuType.MENU);

                // 2.2 在线用户 (MENU)
                long onlineMenuId = saveMenu(monitorDirId, "在线用户", "/monitor/online", "online", "sys:user:online",
                                MenuType.MENU);
                long userKickoutId = saveMenu(onlineMenuId, "强制下线", null, null, "sys:user:kickout", MenuType.BUTTON);

                // 2. Create Roles
                // Admin: All permissions
                long adminRoleId = saveRole("Admin", "admin", "Administrator", DataScope.ALL,
                                java.util.List.of(
                                                sysDirId, userListId, userAddId, userUpdateId, userDeleteId,
                                                roleListId, roleQueryId, roleAddId, roleUpdateId, roleDeleteId,
                                                deptListId, deptAddId, deptUpdateId, deptDeleteId,
                                                menuListId, menuAddId, menuUpdateId, menuDeleteId,
                                                monitorDirId, monitorMenuId, onlineMenuId, userKickoutId));

                // ReadOnly: List only
                long readOnlyRoleId = saveRole("ReadOnly", "readonly", "Read Only User", DataScope.ALL,
                                java.util.List.of(
                                                sysDirId, userListId, roleListId, deptListId, menuListId,
                                                monitorDirId, monitorMenuId, onlineMenuId));

                // 3. Create Departments
                long rootDeptId = saveDept(0, "总部");
                long testDeptId = saveDept(rootDeptId, "测试部门");

                // 4. Create Users
                // Super Admin
                saveUser("admin", "admin@landfun.com", "password", true, adminRoleId, rootDeptId);
                // Normal User
                saveUser("user", "user@landfun.com", "password", false, readOnlyRoleId, testDeptId);

                log.info("RBAC Data seeded successfully with Departments and Users.");
        }

        private long saveDept(long parentId, String name) {
                DeptTable t = DeptTable.$;
                Dept existing = sqlClient.createQuery(t)
                                .where(t.name().eq(name))
                                .select(t)
                                .fetchOneOrNull();

                if (existing != null) {
                        return existing.id();
                }

                return sqlClient.getEntities().save(
                                DeptDraft.$.produce(draft -> {
                                        if (parentId > 0) {
                                                draft.setParentId(parentId);
                                        }
                                        draft.setName(name);
                                })).getModifiedEntity().id();
        }

        private void saveUser(String username, String email, String password, boolean isSuperuser, long roleId,
                        long deptId) {
                UserTable t = UserTable.$;
                User existing = sqlClient.createQuery(t)
                                .where(t.username().eq(username))
                                .select(t)
                                .fetchOneOrNull();

                if (existing != null) {
                        return;
                }

                sqlClient.getEntities().save(
                                UserDraft.$.produce(draft -> {
                                        draft.setUsername(username);
                                        draft.setEmail(email);
                                        draft.setPassword(BCrypt.hashpw(password));
                                        draft.setActive(true);
                                        draft.setSuperuser(isSuperuser);
                                        draft.setRoleId(roleId);
                                        draft.setDeptId(deptId);
                                }));
                log.info("Initialized user: {}", username);
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
                                        if (parentId > 0) {
                                                draft.setParentId(parentId);
                                        } else {
                                                draft.setParent(null);
                                        }
                                        draft.setName(name);
                                        draft.setPath(path);
                                        draft.setIcon(icon);
                                        draft.setPermission(permission);
                                        draft.setType(type);
                                })).getModifiedEntity().id();
        }

        private long saveRole(String name, String code, String description, DataScope dataScope,
                        java.util.List<Long> menuIds) {
                RoleTable t = RoleTable.$;
                Role existing = sqlClient.createQuery(t)
                                .where(t.code().eq(code))
                                .select(t)
                                .fetchOneOrNull();

                if (existing != null) {
                        return existing.id();
                }

                long roleId = sqlClient.getEntities().save(
                                RoleDraft.$.produce(draft -> {
                                        draft.setName(name);
                                        draft.setCode(code);
                                        draft.setDescription(description);
                                        draft.setDataScope(dataScope);
                                })).getModifiedEntity().id();

                // Explicitly sync menus to avoid merging issues
                log.info("Syncing {} menus for role: {}", menuIds.size(), code);
                jdbcTemplate.update("DELETE FROM sys_role_menu_mapping WHERE role_id = ?", roleId);
                for (Long menuId : menuIds) {
                        jdbcTemplate.update("INSERT INTO sys_role_menu_mapping (role_id, menu_id) VALUES (?, ?)",
                                        roleId,
                                        menuId);
                }

                return roleId;
        }

}
