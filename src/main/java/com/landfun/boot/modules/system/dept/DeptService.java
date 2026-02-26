package com.landfun.boot.modules.system.dept;

import java.util.List;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.modules.system.dept.dto.DeptInput;
import com.landfun.boot.modules.system.dept.dto.DeptSpecification;
import com.landfun.boot.modules.system.dept.dto.DeptView;
import com.landfun.boot.modules.system.dept.dto.DeptTreeView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeptService {

    private final JSqlClient sqlClient;

    public PageResult<DeptView> list(DeptSpecification spec, Pageable pageable) {
        Page<DeptView> page = sqlClient.createQuery(DeptTable.$)
                .where(spec)
                .orderBy(SpringOrders.toOrders(DeptTable.$, pageable.getSort()))
                .select(DeptTable.$.fetch(DeptView.class))
                .fetchPage(pageable.getPageNumber(), pageable.getPageSize());
        return PageResult.of(page);
    }

    public List<DeptTreeView> tree() {
        // Fetch all recursive using DTO View
        List<DeptTreeView> roots = sqlClient.createQuery(DeptTable.$)
                .where(org.babyfish.jimmer.sql.ast.Predicate.or(DeptTable.$.parentId().isNull(),
                        DeptTable.$.parentId().eq(0L)))
                .select(
                        DeptTable.$.fetch(DeptTreeView.class))
                .execute();
        return roots;
    }

    @Transactional
    public DeptView save(DeptInput input) {
        SimpleSaveResult<Dept> result = sqlClient.getEntities().save(input);
        return sqlClient.findById(DeptView.class, result.getModifiedEntity().id());
    }

    @Transactional
    public void delete(long id) {
        sqlClient.deleteById(Dept.class, id);
    }
}
