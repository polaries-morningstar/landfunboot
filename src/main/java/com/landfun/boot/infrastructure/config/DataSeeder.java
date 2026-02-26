package com.landfun.boot.infrastructure.config;

import cn.hutool.crypto.digest.BCrypt;
import com.landfun.boot.modules.system.user.UserDraft;
import com.landfun.boot.modules.system.user.UserTable;
import com.landfun.boot.modules.system.role.DataScope;
import com.landfun.boot.modules.system.role.RoleTable;
import com.landfun.boot.modules.system.role.RoleDraft;
import com.landfun.boot.modules.system.role.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private final JSqlClient sqlClient;

        @Override
        public void run(String... args) throws Exception {
                seedRbac();
        }

        private void seedRbac() {
                // 1. Create Menus
                // User Management
                long userListId = saveMenu(0, "User List", "/sys/user", "sys:user:list",
                                com.landfun.boot.modules.system.menu.Menu.Type.MENU);
                long userAddId = saveMenu(userListId, "User Add", null, "sys:user:add",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);
                long userUpdateId = saveMenu(userListId, "User Update", null, "sys:user:update",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);
                long userDeleteId = saveMenu(userListId, "User Delete", null, "sys:user:delete",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);

                // Role Management
                long roleListId = saveMenu(0, "Role List", "/sys/role", "sys:role:list",
                                com.landfun.boot.modules.system.menu.Menu.Type.MENU);
                long roleAddId = saveMenu(roleListId, "Role Add", null, "sys:role:add",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);
                long roleUpdateId = saveMenu(roleListId, "Role Update", null, "sys:role:update",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);
                long roleDeleteId = saveMenu(roleListId, "Role Delete", null, "sys:role:delete",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);

                // Menu Management
                long menuListId = saveMenu(0, "Menu List", "/sys/menu", "sys:menu:list",
                                com.landfun.boot.modules.system.menu.Menu.Type.MENU);
                long menuAddId = saveMenu(menuListId, "Menu Add", null, "sys:menu:add",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);
                long menuUpdateId = saveMenu(menuListId, "Menu Update", null, "sys:menu:update",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);
                long menuDeleteId = saveMenu(menuListId, "Menu Delete", null, "sys:menu:delete",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);

                // Dept Management
                long deptListId = saveMenu(0, "Dept List", "/sys/dept", "sys:dept:list",
                                com.landfun.boot.modules.system.menu.Menu.Type.MENU);
                long deptAddId = saveMenu(deptListId, "Dept Add", null, "sys:dept:add",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);
                long deptUpdateId = saveMenu(deptListId, "Dept Update", null, "sys:dept:update",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);
                long deptDeleteId = saveMenu(deptListId, "Dept Delete", null, "sys:dept:delete",
                                com.landfun.boot.modules.system.menu.Menu.Type.BUTTON);

                // 2. Create Roles
                // Admin: All permissions
                long adminRoleId = saveRole("Admin", "admin", "Administrator", DataScope.ALL,
                                java.util.List.of(userListId, userAddId, userUpdateId, userDeleteId,
                                                roleListId, roleAddId, roleUpdateId, roleDeleteId,
                                                menuListId, menuAddId, menuUpdateId, menuDeleteId,
                                                deptListId, deptAddId, deptUpdateId, deptDeleteId));

                // ReadOnly: List only
                long readOnlyRoleId = saveRole("ReadOnly", "readonly", "Read Only User", DataScope.ALL,
                                java.util.List.of(userListId, roleListId, menuListId));

                // 3. Update Admin User
                updateAdminUser(adminRoleId);
                log.info("RBAC Data seeded successfully.");
        }

        private long saveMenu(long parentId, String name, String path, String permission,
                        com.landfun.boot.modules.system.menu.Menu.Type type) {
                com.landfun.boot.modules.system.menu.MenuTable t = com.landfun.boot.modules.system.menu.MenuTable.$;
                com.landfun.boot.modules.system.menu.Menu existing = null;
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

                return sqlClient.save(
                                com.landfun.boot.modules.system.menu.MenuDraft.$.produce(draft -> {
                                        draft.setParentId(parentId);
                                        draft.setName(name);
                                        draft.setPath(path);
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

                return sqlClient.save(
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
                com.landfun.boot.modules.system.user.User existing = sqlClient.createQuery(t)
                                .where(t.email().eq(email))
                                .select(t)
                                .fetchOneOrNull();

                if (existing == null) {
                        sqlClient.save(
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
                        sqlClient.save(
                                        UserDraft.$.produce(draft -> {
                                                draft.setId(existing.id());
                                                draft.setActive(true);
                                                draft.addIntoRoles(role -> role.setId(roleId));
                                        }));
                }
        }
}
