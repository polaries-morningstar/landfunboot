package com.landfun.boot.modules.system.dept;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeptService {

    private final JSqlClient sqlClient;

    public List<Dept> tree() {
        // Fetch all recursive
        // Note: Jimmer recursive fetcher usually starts from roots
        List<Dept> roots = sqlClient.createQuery(DeptTable.$)
                .where(DeptTable.$.parentId().isNull().or(DeptTable.$.parentId().eq(0L)))
                .select(
                        DeptTable.$.fetch(
                                DeptFetcher.$
                                        .allScalarFields()
                                        .children(
                                                DeptFetcher.$.allScalarFields())))
                .execute();
        return roots;
    }

    @Transactional
    public long save(DeptInput input) {
        SimpleSaveResult<Dept> result = sqlClient.save(input);
        return result.getModifiedEntity().id();
    }

    @Transactional
    public void delete(long id) {
        sqlClient.deleteById(Dept.class, id);
    }
}
