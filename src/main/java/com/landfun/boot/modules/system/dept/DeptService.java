package com.landfun.boot.modules.system.dept;

import java.util.List;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landfun.boot.infrastructure.exception.BizException;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.modules.system.dept.dto.CreateDeptInput;
import com.landfun.boot.modules.system.dept.dto.UpdateDeptInput;
import com.landfun.boot.modules.system.dept.dto.DeptSpecification;
import com.landfun.boot.modules.system.dept.dto.DeptView;
import com.landfun.boot.modules.system.dept.dto.DeptTreeView;
import com.landfun.boot.modules.system.user.UserTable;

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
    public DeptView create(CreateDeptInput input) {
        SimpleSaveResult<Dept> result = sqlClient.getEntities().save(input);
        return sqlClient.findById(DeptView.class, result.getModifiedEntity().id());
    }

    @Transactional
    public DeptView update(UpdateDeptInput input) {
        sqlClient.getEntities().saveCommand(input)
                .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.UPDATE_ONLY)
                .execute();
        return sqlClient.findById(DeptView.class, input.getId());
    }

    @Transactional
    public void delete(long id) {
        // 1. 不允许删除仍有未删除子部门的部门
        long childCount = sqlClient
                .filters(cfg -> cfg.disableAll())
                .createQuery(DeptTable.$)
                .where(
                        DeptTable.$.parentId().eq(id),
                        DeptTable.$.deleteTime().isNull())
                .select(DeptTable.$.id().count())
                .fetchOne();
        if (childCount > 0) {
            throw new BizException("当前部门仍包含子部门，无法删除，请先删除或迁移子部门");
        }

        // 2. 不允许删除仍有未删除用户挂靠的部门
        long userCount = sqlClient
                .filters(cfg -> cfg.disableAll())
                .createQuery(UserTable.$)
                .where(
                        UserTable.$.deptId().eq(id),
                        UserTable.$.deleteTime().isNull())
                .select(UserTable.$.id().count())
                .fetchOne();
        if (userCount > 0) {
            throw new BizException("当前部门下仍有用户，无法删除，请先调整用户所属部门");
        }

        sqlClient.deleteById(Dept.class, id);
    }
}
