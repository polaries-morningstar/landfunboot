package com.landfun.boot.modules.system.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.modules.system.menu.dto.CreateMenuInput;
import com.landfun.boot.modules.system.menu.dto.UpdateMenuInput;
import com.landfun.boot.modules.system.menu.dto.MenuSpecification;
import com.landfun.boot.modules.system.menu.dto.MenuView;
import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final JSqlClient sqlClient;
    private final StringRedisTemplate redisTemplate;

    public PageResult<MenuView> list(MenuSpecification spec, Pageable pageable) {
        Page<MenuView> page = sqlClient.createQuery(MenuTable.$)
                .where(spec)
                .orderBy(SpringOrders.toOrders(MenuTable.$, pageable.getSort()))
                .select(MenuTable.$.fetch(MenuView.class))
                .fetchPage(pageable.getPageNumber(), pageable.getPageSize());
        return PageResult.of(page);
    }

    /** All menus with parent loaded, for building full tree (e.g. for superuser). */
    public List<Menu> getAllMenusWithParent() {
        return sqlClient.createQuery(MenuTable.$)
                .select(MenuTable.$.fetch(
                        MenuFetcher.$.allScalarFields().parent(MenuFetcher.$.allScalarFields())))
                .execute();
    }

    public List<java.util.Map<String, Object>> tree() {
        List<Menu> allMenus = getAllMenusWithParent();
        java.util.Map<Long, java.util.Map<String, Object>> map = new java.util.HashMap<>();
        java.util.List<java.util.Map<String, Object>> roots = new java.util.ArrayList<>();

        for (Menu m : allMenus) {
            java.util.Map<String, Object> node = new java.util.LinkedHashMap<>();
            node.put("id", m.id());
            node.put("name", m.name());
            node.put("path", m.path());
            node.put("icon", m.icon());
            node.put("permission", m.permission());
            node.put("type", m.type());
            node.put("createdTime", m.createdTime());
            node.put("updatedTime", m.updatedTime());
            node.put("children", new java.util.ArrayList<>());
            map.put(m.id(), node);
        }

        for (Menu m : allMenus) {
            java.util.Map<String, Object> current = map.get(m.id());
            Menu parent = m.parent();
            if (parent == null) {
                roots.add(current);
            } else {
                java.util.Map<String, Object> parentNode = map.get(parent.id());
                if (parentNode != null) {
                    ((java.util.List<Object>) parentNode.get("children")).add(current);
                } else {
                    roots.add(current);
                }
            }
        }
        return roots;
    }

    /**
     * Build menu tree from a subset of menus (e.g. current user's role menus).
     * Only DIR and MENU types are included; BUTTON is excluded.
     */
    public List<Map<String, Object>> treeFromMenus(List<Menu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> idSet = menus.stream().map(Menu::id).collect(Collectors.toSet());
        List<Menu> dirAndMenu = menus.stream()
                .filter(m -> m.type() == MenuType.DIR || m.type() == MenuType.MENU)
                .toList();
        Map<Long, Map<String, Object>> map = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Menu m : dirAndMenu) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.id());
            node.put("name", m.name());
            node.put("path", m.path());
            node.put("icon", m.icon());
            node.put("permission", m.permission());
            node.put("type", m.type());
            node.put("children", new ArrayList<>());
            map.put(m.id(), node);
        }
        for (Menu m : dirAndMenu) {
            Map<String, Object> current = map.get(m.id());
            Menu parent = m.parent();
            if (parent == null || !idSet.contains(parent.id())) {
                roots.add(current);
            } else {
                Map<String, Object> parentNode = map.get(parent.id());
                if (parentNode != null) {
                    @SuppressWarnings("unchecked")
                    List<Object> children = (List<Object>) parentNode.get("children");
                    children.add(current);
                } else {
                    roots.add(current);
                }
            }
        }
        return roots;
    }

    @Transactional
    public MenuView create(CreateMenuInput input) {
        SimpleSaveResult<Menu> result = sqlClient.getEntities()
                .saveCommand(input)
                .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.INSERT_ONLY)
                .execute();
        return sqlClient.findById(MenuView.class, result.getModifiedEntity().id());
    }

    @Transactional
    public MenuView update(UpdateMenuInput input) {
        SimpleSaveResult<Menu> result = sqlClient.getEntities()
                .saveCommand(input)
                .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.UPDATE_ONLY)
                .execute();
        clearAllUserPermissionCaches();
        return sqlClient.findById(MenuView.class, result.getModifiedEntity().id());
    }

    @Transactional
    public void delete(long id) {
        sqlClient.deleteById(Menu.class, id);
        clearAllUserPermissionCaches();
    }

    private void clearAllUserPermissionCaches() {
        java.util.Set<String> keys = redisTemplate.keys("user:permissions:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
