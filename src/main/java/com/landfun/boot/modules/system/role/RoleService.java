package com.landfun.boot.modules.system.role;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.modules.system.role.dto.RoleInput;
import com.landfun.boot.modules.system.role.dto.RoleSpecification;
import com.landfun.boot.modules.system.role.dto.RoleView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final JSqlClient sqlClient;

    public PageResult<RoleView> page(RoleSpecification spec, Pageable pageable) {
        Page<RoleView> page = sqlClient.createQuery(RoleTable.$)
                .where(spec)
                .orderBy(SpringOrders.toOrders(RoleTable.$, pageable.getSort()))
                .select(RoleTable.$.fetch(RoleView.class))
                .fetchPage(pageable.getPageNumber(), pageable.getPageSize());
        return PageResult.of(page);
    }

    @Transactional
    public RoleView save(RoleInput input) {
        SimpleSaveResult<Role> result = sqlClient.getEntities().save(input);
        return sqlClient.findById(RoleView.class, result.getModifiedEntity().id());
    }

    @Transactional
    public void delete(long id) {
        sqlClient.deleteById(Role.class, id);
    }

    public java.util.List<RoleView> listAll(RoleSpecification spec, Pageable pageable) {
        return sqlClient.createQuery(RoleTable.$)
                .where(spec)
                .orderBy(SpringOrders.toOrders(RoleTable.$, pageable.getSort()))
                .select(RoleTable.$.fetch(RoleView.class))
                .execute();
    }

    public RoleView findById(long id) {
        return sqlClient.findById(RoleView.class, id);
    }
}
