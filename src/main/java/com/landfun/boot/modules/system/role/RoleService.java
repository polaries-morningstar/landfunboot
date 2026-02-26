package com.landfun.boot.modules.system.role;

import com.landfun.boot.infrastructure.web.BasePageQuery;
import com.landfun.boot.infrastructure.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final JSqlClient sqlClient;

    public PageResult<Role> page(BasePageQuery query) {
        Page<Role> page = sqlClient.createQuery(RoleTable.$)
                .select(RoleTable.$)
                .fetchPage(query.pageIndex(), query.pageSize());
        return PageResult.of(page);
    }

    @Transactional
    public long save(RoleInput input) {
        SimpleSaveResult<Role> result = sqlClient.save(input);
        return result.getModifiedEntity().id();
    }

    @Transactional
    public void delete(long id) {
        sqlClient.deleteById(Role.class, id);
    }

    public java.util.List<Role> listAll() {
        return sqlClient.createQuery(RoleTable.$)
                .select(RoleTable.$)
                .execute();
    }

    public Role findById(long id) {
        return sqlClient.findById(RoleFetcher.$.allScalarFields()
                .depts(com.landfun.boot.modules.system.dept.DeptFetcher.$.allScalarFields())
                .menus(com.landfun.boot.modules.system.menu.MenuFetcher.$.allScalarFields()), id);
    }
}
