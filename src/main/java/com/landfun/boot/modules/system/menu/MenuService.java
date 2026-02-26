package com.landfun.boot.modules.system.menu;

import java.util.List;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.modules.system.menu.dto.MenuInput;
import com.landfun.boot.modules.system.menu.dto.MenuSpecification;
import com.landfun.boot.modules.system.menu.dto.MenuView;
import com.landfun.boot.modules.system.menu.dto.MenuTreeView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final JSqlClient sqlClient;

    public PageResult<MenuView> list(MenuSpecification spec, Pageable pageable) {
        Page<MenuView> page = sqlClient.createQuery(MenuTable.$)
                .where(spec)
                .orderBy(SpringOrders.toOrders(MenuTable.$, pageable.getSort()))
                .select(MenuTable.$.fetch(MenuView.class))
                .fetchPage(pageable.getPageNumber(), pageable.getPageSize());
        return PageResult.of(page);
    }

    public List<MenuTreeView> tree() {
        return sqlClient.createQuery(MenuTable.$)
                .where(org.babyfish.jimmer.sql.ast.Predicate.or(
                        MenuTable.$.parentId().isNull(),
                        MenuTable.$.parentId().eq(0L)))
                .select(MenuTable.$.fetch(MenuTreeView.class))
                .execute();
    }

    @Transactional
    public MenuView save(MenuInput input) {
        SimpleSaveResult<Menu> result = sqlClient.getEntities().save(input);
        return sqlClient.findById(MenuView.class, result.getModifiedEntity().id());
    }

    @Transactional
    public void delete(long id) {
        sqlClient.deleteById(Menu.class, id);
    }
}
