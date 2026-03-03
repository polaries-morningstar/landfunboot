package com.landfun.boot.modules.system.dept;

import java.util.SortedMap;
import java.util.TreeMap;

import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.stereotype.Component;

import com.landfun.boot.infrastructure.service.DataScopeResolver;
import com.landfun.boot.infrastructure.service.DataScopeResolver.ScopeResult;
import com.landfun.boot.infrastructure.service.DataScopeService;
import com.landfun.boot.infrastructure.web.AuthContext;
import com.landfun.boot.modules.system.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeptFilter implements CacheableFilter<DeptProps> {

    private final DataScopeService dataScopeService;

    @Override
    public SortedMap<String, Object> getParameters() {
        User user = AuthContext.getUser();
        if (user == null) {
            return null;
        }
        SortedMap<String, Object> map = new TreeMap<>();
        map.put("userId", user.id());
        return map;
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        return e.getImmutableType().getJavaClass() == Dept.class;
    }

    @Override
    public void filter(FilterArgs<DeptProps> args) {
        User user = AuthContext.getUser();
        if (user == null) {
            return;
        }

        ScopeResult scope = DataScopeResolver.resolve(user, dataScopeService::getSubDeptIds);

        if (scope.unrestricted()) {
            return;
        }

        if (scope.selfOnly()) {
            Long userDeptId = (user.dept() != null) ? user.dept().id() : null;
            if (userDeptId != null) {
                args.where(args.getTable().id().eq(userDeptId));
            } else {
                args.where(args.getTable().id().eq(-1L));
            }
            return;
        }

        args.where(args.getTable().id().in(scope.allowedDeptIds()));
    }
}
